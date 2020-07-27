package trivia.repository;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.micronaut.context.annotation.Requires;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.domain.RoundEvent;

import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Requires(beans = RedisClient.class)
@Singleton
public class RedisGameRepository implements GameRepository {

    // keys
    static final String Q_KEY_PREFIX = "q:";
    static final String GAME_KEY_PREFIX = "game:";
    static final String PLAYERS_KEY_PREFIX = "players:";
    static final String ROUNDS_KEY_PREFIX = "rounds:";
    static final String GAME_PENDING_KEY = "games_pending:";
    static final String DELAYED_ROUNDS_KEY = "delayed:rounds:";
    static final String GAME_CHANNEL_KEY = "game_channel:";
    static final String ROUNDS_CHANNEL_KEY = "rounds_channel:";

    // FIELDS
    static final String TITLE = "title";
    static final String TOTAL_ROUNDS = "nrounds";
    static final String ROUND = "round";
    static final String QUESTION = "question";
    static final String PLAYERS = "players";

    private final StatefulRedisConnection<String,String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final JsonEncoder encoder;


    public RedisGameRepository(StatefulRedisConnection<String,String> connection, StatefulRedisPubSubConnection<String, String> pubSubConnection, JsonEncoder encoder) {
        this.connection = Objects.requireNonNull(connection);
        this.pubSubConnection = Objects.requireNonNull(pubSubConnection);
        this.encoder = Objects.requireNonNull(encoder);
    }

    @Override
    public Mono<Long> save(Question question) {
        // add question to a sorted set, keyed by the category name
        return Mono.fromSupplier(() -> encoder.encodeQuestion(question))
            .flatMap(q -> connection.reactive().zadd(questionKey(question.getCategory()), 1, q));
    }

    @Override
    public Flux<Question> findQuestionsInCategory(String category) {
        return findQuestionsInCategory(category, -1);
    }

    @Override
    public Flux<Question> findQuestionsInCategory(String category, int stop) {
        return connection.reactive().zrange(questionKey(category), 0, stop)
            .map(encoder::decodeQuestion);
    }

    @Override
    public Flux<Question> allocateQuestions(String category, int stop) {
        var commands = this.connection.reactive();
        String key = questionKey(category);
        long timestamp = Instant.now().toEpochMilli();
        return commands.zrange(key, 0, stop)
            // score the question with the timestamp so it pushes to the bottom and we don't see the same one every time
            .doOnNext(member -> commands.zadd(key, timestamp, member).subscribe())
            .map(encoder::decodeQuestion);
    }

    @Override
    public Flux<String> listCategories() {
        // get the keys matching the question index
        Flux<String> keys = connection.reactive().keys(questionKey("*"));
        return keys.map(s -> s.substring(Q_KEY_PREFIX.length()));
    }

    @Override
    public Mono<String> createGame(String title, List<Question> questions) {
        // allocate a game id, then create a game hash entry for the game
        var commands = connection.reactive();
        return commands.incr(GAME_KEY_PREFIX).flatMap(id -> {
            final String gameId = id.toString();
            String gameKey = gameKey(gameId);
            // transaction to create game and rounds
            return commands.watch(gameKey).flatMap(watchResp -> commands.multi().flatMap(multiResp -> {
                commands.hset(gameKey, Map.of(TITLE, title, TOTAL_ROUNDS, String.valueOf(questions.size()))).subscribe();
                for (int i=0; i < questions.size(); i++) {
                    String encoded = encoder.encodeQuestion(questions.get(i));
                    commands.hset(roundsKey(gameId, i), QUESTION, encoded).subscribe();
                }
                commands.exec().subscribe();
                return Mono.just(gameId);
            }));
        });
    }

    @Override
    public Mono<Game> findGame(String gameId) {
        return findGameWithCommands(gameId, this.connection.reactive());
    }

    /*
     * Utility method to allow for reuse and pipelining by giving an instance of RedisReactiveCommands
     */
    private Mono<Game> findGameWithCommands(String gameId, RedisReactiveCommands<String, String> commands) {
        return commands.scard(playersKey(gameId))        // count players set, O(1)
            .zipWith(commands.hgetall(gameKey(gameId)))  // find game fields
            .filter(tuple -> !tuple.getT2().isEmpty())   // empty map is not found
            .map(tuple -> Game.builder()
                .id(gameId)
                .title(tuple.getT2().get(TITLE))
                .round(tuple.getT2().containsKey(ROUND) ? Integer.parseInt(tuple.getT2().get(ROUND)) : null)
                .players(tuple.getT1().intValue())
                .build());
    }

    @Override
    public Mono<Question> findQuestionForRound(String gameId, int round) {
        var commands = connection.reactive();
        return commands.hget(gameKey(gameId) + ":round:" + round, QUESTION)
            .map(encoder::decodeQuestion);
    }

    @Override
    public void countAnswer(String gameId, int round, String answer) {
        var commands = connection.reactive();
        commands.hincrby(roundsKey(gameId, round), answer, 1);
    }

    @Override
    public Mono<Integer> findPlayerCount(String gameId, int round) {
        throw new UnsupportedOperationException("fix me");
    }

    @Override
    public Mono<Game> addPlayer(String gameId, String username) {
        var commands = connection.reactive();
        return commands.hgetall(gameKey(gameId))
            .filter(g -> !g.isEmpty() && !g.containsKey(ROUND))
            .flatMap(g -> commands.sadd(playersKey(gameId), username))
            .filter(n -> n > 0L) // if already registered
            .flatMap(g -> findGameWithCommands(gameId, commands))
            .doOnSuccess(g -> {
                if (g != null) {
                    ScoredValue<String> scoredGame = ScoredValue.fromNullable(g.getPlayers(), gameId);
                    commands.zadd(GAME_PENDING_KEY, scoredGame).subscribe();
                }
            });
    }

    @Override
    public void advancePendingRounds(Duration roundStartDelay, Duration roundDuration) {
        // This supports polling so use sync for clarity
        RedisCommands<String, String> commands = connection.sync();
        final double time = Instant.now().toEpochMilli();
        Range<Double> range = Range.from(Range.Boundary.including(time), Range.Boundary.unbounded());
        List<String> pending = commands.zrangebyscore(DELAYED_ROUNDS_KEY, range, Limit.from(100));

        // FIXME: locks
        pending.forEach(value -> {
            RoundEvent roundEvent = encoder.decodeRoundEvent(value);
            String roundsKey = roundsKey(roundEvent.getGameId(), roundEvent.getRound());
            String gameKey = gameKey(roundEvent.getGameId());
            String playersKey = playersKey(roundEvent.getGameId());
            commands.watch(roundsKey);
            commands.multi();
            if (roundEvent.getStarted()) {
                commands.hincrby(gameKey, ROUND, 1);
                Long currentPlayers = commands.scard(playersKey);
                commands.hset(roundsKey, PLAYERS, String.valueOf(currentPlayers));
                RoundEvent nextEvent = RoundEvent.builder()
                    .gameId(roundEvent.getGameId())
                    .round(roundEvent.getRound())
                    .started(true)
                    .build();
                commands.zadd(DELAYED_ROUNDS_KEY, ScoredValue.just(futureTimestamp(roundDuration), encoder.encodeRoundEvent(nextEvent)));
            } else {
                int max = Integer.parseInt(commands.hget(gameKey, TOTAL_ROUNDS));
                int nextRoundNumber = roundEvent.getRound() + 1;
                if (nextRoundNumber < max) {
                    RoundEvent nextEvent = RoundEvent.builder()
                        .gameId(roundEvent.getGameId())
                        .round(nextRoundNumber)
                        .started(true)
                        .build();
                    commands.zadd(DELAYED_ROUNDS_KEY, ScoredValue.just(futureTimestamp(roundStartDelay), encoder.encodeRoundEvent(nextEvent)));
                }
            }
            commands.zrem(DELAYED_ROUNDS_KEY, value);
            commands.publish(ROUNDS_CHANNEL_KEY, value);
            commands.exec();
        });
    }

    @Override
    public void startPendingGames(Duration roundStartDelay, int minPlayers) {
        // This supports polling so use sync for clarity
        RedisCommands<String, String> commands = connection.sync();

        List<String> pending = commands.zrangebyscore(GAME_PENDING_KEY,
            Range.from(Range.Boundary.including(minPlayers), Range.Boundary.unbounded()),
            Limit.from(100));

        pending.forEach(gameId -> {
            String gameKey = gameKey(gameId);
            commands.watch(gameKey);
            commands.multi();
            commands.zrem(GAME_PENDING_KEY, gameId);
            RoundEvent roundEvent = RoundEvent.builder()
                .gameId(gameId)
                .round(0)
                .started(true)
                .build();
            commands.zadd(DELAYED_ROUNDS_KEY, ScoredValue.just(futureTimestamp(roundStartDelay), encoder.encodeRoundEvent(roundEvent)));
            commands.publish(GAME_CHANNEL_KEY, gameId);
            commands.exec();
        });
    }

    @Override
    public Flux<String> subscribeToGameChannel() {
        RedisPubSubReactiveCommands<String, String> commands = subscribeToChannel(GAME_CHANNEL_KEY);
        return commands.observeChannels().map(channelMessage -> {
            log.debug("Channel[{}] {}", channelMessage.getChannel(), channelMessage.getMessage());
            return channelMessage.getMessage();
        });
    }

    @Override
    public Flux<RoundEvent> subscribeToRoundsChannel() {
        RedisPubSubReactiveCommands<String, String> commands = subscribeToChannel(ROUNDS_CHANNEL_KEY);
        return commands.observeChannels().map(channelMessage -> {
            log.debug("Channel[{}] {}", channelMessage.getChannel(), channelMessage.getMessage());
            return encoder.decodeRoundEvent(channelMessage.getMessage());
        });
    }

    private RedisPubSubReactiveCommands<String, String> subscribeToChannel(String channelKey) {
        RedisPubSubReactiveCommands<String, String> commands = pubSubConnection.reactive();
        commands.subscribe(channelKey)
            .doOnNext(msg -> log.debug("Channel[{}] {}", channelKey, msg))
            .subscribe();
        return commands;
    }

    private long futureTimestamp(Duration delay) {
        return Instant.now().plusMillis(delay.toMillis()).toEpochMilli();
    }

    private String questionKey(String category) {
        return String.format("%s%s", Q_KEY_PREFIX, category);
    }

    private String gameKey(String gameId) {
        return String.format("%s%s", GAME_KEY_PREFIX, gameId);
    }

    private String playersKey(String gameId) {
        return String.format("%s%s", PLAYERS_KEY_PREFIX, gameId);
    }

    private String roundsKey(String gameId, int round) {
        return String.format("%s%s:%d", ROUNDS_KEY_PREFIX, gameId, round);
    }

    private String gameChannel(String gameId) {
        return String.format("%s%s", GAME_CHANNEL_KEY, gameId);
    }
}
