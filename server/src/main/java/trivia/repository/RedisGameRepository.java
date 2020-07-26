package trivia.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;

@Slf4j
@Requires(beans = RedisClient.class)
@Singleton
public class RedisGameRepository implements GameRepository {

    // keys
    static final String Q_KEY_PREFIX = "q:";
    static final String GAME_KEY_PREFIX = "game:";
    static final String PLAYERS_KEY_PREFIX = "players:";
    static final String GAME_PENDING_KEY = "games_pending:";
    static final String GAME_CHANNEL_PREFIX = "game_channel:";

    // FIELDS
    static final String CATEGORY = "category";
    static final String STARTED = "started";

    private final StatefulRedisConnection<String,String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private final ObjectMapper mapper;

    public RedisGameRepository(StatefulRedisConnection<String,String> connection, StatefulRedisPubSubConnection<String, String> pubSubConnection, ObjectMapper mapper) {
        this.connection = Objects.requireNonNull(connection);
        this.pubSubConnection = Objects.requireNonNull(pubSubConnection);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Mono<Double> save(Question question) {
        // add question to a sorted set, keyed by the category name
        return Mono.fromSupplier(() -> encodeQuestion(question))
            .flatMap(q -> connection.reactive().zaddincr(questionKey(question.getCategory()),1.0D, q));
    }

    @Override
    public Flux<Question> findQuestionsInCategory(String category) {
        return connection.reactive().zrange(questionKey(category), 0, -1)
            .map(this::decodeQuestion);
    }

    @Override
    public Flux<String> listCategories() {
        // get the keys matching the question index
        Flux<String> keys = connection.reactive().keys(questionKey("*"));
        return keys.map(s -> s.substring(Q_KEY_PREFIX.length()));
    }

    @Override
    public Mono<String> createGame(String category) {
        // allocate a game id, then create a game hash entry for the game
        RedisReactiveCommands<String, String> commands = connection.reactive();
        return commands.incr(GAME_KEY_PREFIX).flatMap(id -> {
            final String gameId = id.toString();
            return commands.hset(gameKey(gameId), CATEGORY, category)
                .map(b -> gameId);
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
                .category(tuple.getT2().get(CATEGORY))
                .started(Boolean.parseBoolean(tuple.getT2().getOrDefault(STARTED, "false")))
                .players(tuple.getT1().intValue())
                .build());
    }

    @Override
    public void startPendingGames(int minPlayers) {

        // This supports polling, so no need for async
        RedisCommands<String, String> commands = connection.sync();

        List<String> pending = commands.zrangebyscore(GAME_PENDING_KEY,
            Range.from(Range.Boundary.including(minPlayers), Range.Boundary.unbounded()),
            Limit.from(100));

        pending.forEach(gameId -> {
            commands.multi();
            commands.hset(gameKey(gameId), STARTED, "true");
            commands.zrem(GAME_PENDING_KEY, gameId);
            commands.publish(GAME_CHANNEL_PREFIX, gameId);
            //... round?
            commands.exec();
        });
    }

    @Override
    public Mono<Game> addPlayer(String gameId, String username) {
        var commands = connection.reactive();
        return commands.hgetall(gameKey(gameId))
            .filter(g -> !g.isEmpty() && !g.containsKey(STARTED))
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
    public Flux<String> subscribeToGameChannel() {
        RedisPubSubReactiveCommands<String, String> commands = pubSubConnection.reactive();
        commands.subscribe(GAME_CHANNEL_PREFIX)
            .doOnNext(msg -> log.debug("Channel[{}] {}", GAME_CHANNEL_PREFIX, msg))
            .subscribe();

        return commands.observeChannels().map(channelMessage -> {
            log.debug("Channel[{}] {}", channelMessage.getChannel(), channelMessage.getMessage());
            return channelMessage.getMessage();
        });
    }

    private Question decodeQuestion(String data) {
        try {
            return mapper.readValue(data, Question.class);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to decode Question from repository", e);
        }
    }

    private String encodeQuestion(Question q) {
        try {
            return mapper.writeValueAsString(q);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to encode Question to repository", e);
        }
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

    private String gameChannel(String gameId) {
        return String.format("%s%s", GAME_CHANNEL_PREFIX, gameId);
    }
}
