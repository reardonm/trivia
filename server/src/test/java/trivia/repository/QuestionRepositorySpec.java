package trivia.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import trivia.domain.Difficulty;
import trivia.domain.Question;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuestionRepositorySpec implements TestPropertyProvider {

    @Container
    public GenericContainer<?> redis = new GenericContainer<>("redis:5.0.3-alpine")
        .withExposedPorts(6379);

    @Inject
    StatefulRedisConnection<String,String> connection;

    @Inject
    ObjectMapper mapper;

    RedisQuestionRepository underTest;

    @BeforeEach
    public void setUp() {
        // Assume that we have Redis running locally?
        underTest = new RedisQuestionRepository(connection, mapper);
    }

    @Test
    void roundTripData() throws Exception {
        Question question = Question.builder()
            .id("100")
            .category("Math")
            .difficulty(Difficulty.Easy)
            .question("What is 2 + 2")
            .correctAnswer("4")
            .incorrectAnswers(List.of("3","42","Donkey"))
            .build();

        assertThat(underTest.save(question).block()).isTrue();

        Optional<Question> value = underTest.find(question.getId()).blockOptional();
        assertThat(value).hasValueSatisfying(q -> {
            assertThat(q).isEqualTo(question);
        });
    }

    @Nonnull
    @Override
    public Map<String, String> getProperties() {
        redis.start();
        assertThat(redis.isRunning()).isTrue();
        return Map.of(
            "redis.uri", String.format("redis://%s:%d", redis.getHost(), redis.getFirstMappedPort()));
    }
}
