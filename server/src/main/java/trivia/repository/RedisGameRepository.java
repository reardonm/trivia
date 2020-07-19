package trivia.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.micronaut.context.annotation.Requires;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;

import javax.inject.Singleton;
import java.util.Objects;

@Slf4j
@Requires(beans = RedisClient.class)
@Singleton
public class RedisGameRepository implements GameRepository {

    static final String Q_KEY_PREFIX = "q:";
    static final String GAME_KEY_PREFIX = "game:";
    static final String PLAYERS_KEY_PREFIX = "players:";
    static final String CATEGORY = "category";
    static final String PLAYERS = "players";
    static final String STARTED = "started";

    private final StatefulRedisConnection<String,String> connection;
    private final ObjectMapper mapper;

    public RedisGameRepository(StatefulRedisConnection<String,String> connection, ObjectMapper mapper) {
        this.connection = Objects.requireNonNull(connection);
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
        return connection.reactive().incr(GAME_KEY_PREFIX).flatMap(id -> {
            final String gameId = id.toString();
            return connection.reactive().hset(gameKey(gameId), CATEGORY, category).map(b -> gameId);
        });
    }

    @Override
    public Mono<Game> findGame(String gameId) {
        var commands = this.connection.reactive();
        return commands.scard(playersKey(gameId))        // count players set
            .zipWith(commands.hgetall(gameKey(gameId)))  // find game fields
            .filter(tuple -> !tuple.getT2().isEmpty())   // empty map is not found
            .map(tuple -> Game.builder()
                .id(gameId)
                .category(tuple.getT2().get(CATEGORY))
                .players(tuple.getT1().intValue())
                .started(Boolean.parseBoolean(tuple.getT2().getOrDefault(STARTED, "false")))
                .build());
    }

    @Override
    public Mono<Long> addPlayer(String gameId, String username) {
        return connection.reactive().sadd(playersKey(gameId), username);
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
}
