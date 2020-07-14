package trivia.api;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.websocket.RxWebSocketClient;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
public class WebSocketGameHandlerSpec {

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    @Client("/api")
    private RxWebSocketClient wsClient;

    @Inject
    private WebSocketBroadcaster webSocketBroadcaster;

    @Test
    public void join_game() throws Exception {
        var gameId = "100";
        var userName = "alice";
        var uri = String.format("/games/%s/%s", gameId, userName);

        // join the game
        GameTestClient gameTestClient = this.wsClient.connect(GameTestClient.class, uri).blockingFirst();
        assertThat(gameTestClient.getSession().isOpen()).isTrue();
        assertThat(gameTestClient.getGameId()).isEqualTo(gameId);
        assertThat(gameTestClient.getUsername()).isEqualTo(userName);

        // send three messages, expect acks
        gameTestClient.send("FOO");
        gameTestClient.send("BAZ");
        gameTestClient.send("BAZ");

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            // joined message + the acks
            assertThat(assertThat(gameTestClient.getReceived()).hasSize(4))
        );

        gameTestClient.close();
    }
}
