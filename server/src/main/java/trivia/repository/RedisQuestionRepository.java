package trivia.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;
import reactor.core.publisher.Mono;
import trivia.domain.Question;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class RedisQuestionRepository implements QuestionRepository {

    private final StatefulRedisConnection<String,String> connection;
    private final ObjectMapper mapper;

    public RedisQuestionRepository(StatefulRedisConnection<String,String> connection, ObjectMapper mapper) {
        this.connection = Objects.requireNonNull(connection);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Mono<Boolean> save(Question question) {
        return Mono.fromSupplier(() -> encode(question))
            .flatMap(q -> connection.reactive().set(question.getId(), q))
            .map("OK"::equals);
    }

    @Override
    public Mono<Question> find(String key) {
        return connection.reactive().get(key)
            .map(this::decode);
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
