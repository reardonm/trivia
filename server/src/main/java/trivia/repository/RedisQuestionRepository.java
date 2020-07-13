package trivia.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.context.annotation.Requires;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Question;

import javax.inject.Singleton;
import java.util.Objects;

@Requires(beans = RedisClient.class)
@Singleton
public class RedisQuestionRepository implements QuestionRepository {

    private final StatefulRedisConnection<String,String> connection;
    private final ObjectMapper mapper;

    public RedisQuestionRepository(StatefulRedisConnection<String,String> connection, ObjectMapper mapper) {
        this.connection = Objects.requireNonNull(connection);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Mono<Double> save(Question question) {
        // add question to a sorted set, keyed by the category name
        return Mono.fromSupplier(() -> encode(question))
            .flatMap(q -> connection.reactive().zaddincr(createKey(question),1.0D, q));
    }

    private String createKey(Question question) {
        return String.format("q__%s", question.getCategory());
    }

    @Override
    public Flux<Question> find(String category) {
        return connection.reactive().zrange("q__"+category, 0, -1)
            .map(this::decode);
    }

    @Override
    public Flux<String> listCategories() {
        // get the keys matching the question index
        Flux<String> keys = connection.reactive().keys("q__*");
        return keys.map(s -> s.substring(3));
    }

    private Question decode(String data) {
        try {
            return mapper.readValue(data, Question.class);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to decode Question from repository", e);
        }
    }

    private String encode(Question q) {
        try {
            return mapper.writeValueAsString(q);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to encode Question to repository", e);
        }
    }
}
