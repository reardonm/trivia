package trivia;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegerationTestSupport implements TestPropertyProvider {

    @Container
    public GenericContainer<?> redis = new GenericContainer<>("redis:5.0.3-alpine")
        .withExposedPorts(6379);

    @Inject
    protected StatefulRedisConnection<String,String> connection;

    @Inject
    protected ObjectMapper mapper;

    @Nonnull
    @Override
    public Map<String, String> getProperties() {
        // get container ports after start
        redis.start();
        assertThat(redis.isRunning()).isTrue();
        // override redis URL to container
        return Map.of("redis.uri", String.format("redis://%s:%d", redis.getHost(), redis.getFirstMappedPort()));
    }

}
