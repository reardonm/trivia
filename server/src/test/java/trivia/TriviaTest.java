package trivia;

import io.micronaut.runtime.EmbeddedApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import trivia.repository.QuestionRepository;

import javax.inject.Inject;

public class TriviaTest extends IntegerationTestSupport {

    @Inject
    EmbeddedApplication application;

    @Inject
    QuestionRepository queryRepository;

    @Test
    void testItWorks() throws Exception {
        Assertions.assertTrue(application.isRunning());
    }
}
