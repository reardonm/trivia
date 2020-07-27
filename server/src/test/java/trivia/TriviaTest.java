package trivia;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.websocket.RxWebSocketClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import trivia.repository.GameRepository;

import javax.inject.Inject;

public class TriviaTest extends IntegerationTestSupport {

    @Inject
    EmbeddedApplication application;

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    @Client("/api")
    private RxWebSocketClient wsClient;

    @Test
    void simpleGame() throws Exception {
        Assertions.assertTrue(application.isRunning());
    }
}
