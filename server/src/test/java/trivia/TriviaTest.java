package trivia;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.websocket.RxWebSocketClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import trivia.api.GameTestClient;
import trivia.repository.RedisGameRepository;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "application-test.yml")
public class TriviaTest extends IntegerationTestSupport {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    RedisGameRepository repository;

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    @Client("/api")
    private RxWebSocketClient wsClient;

    @Test
    void simpleGame() throws Exception {
        Assertions.assertTrue(application.isRunning());

        // stage test data
        TestData testData = TestData.load(mapper);
        testData.getQuestions().forEach(q -> repository.save(q).block());

        // alice creates game
        var response = client.toBlocking().exchange(HttpRequest.POST("/games", Map.of("category", "Entertainment: Video Games")), String.class);
        assertThat(response.getStatus().getCode()).isEqualTo(201);
        ReadContext ctx = JsonPath.parse(response.body());
        var gameId = ctx.<String>read("$.gameId");
        assertThat(gameId).isNotBlank();

        // alice joins game w websocket
        var aliceUsername = "alice";
        GameTestClient aliceGameClient = this.wsClient.connect(GameTestClient.class, String.format("/games/%s/%s", gameId, aliceUsername)).blockingFirst();
        assertThat(aliceGameClient.getSession().isOpen()).isTrue();
        awaitReceivedMessages(aliceGameClient, 1);
        expectMessageType(aliceGameClient, "player_joined");

        // bob joins game
        var bobUsername = "bob";
        GameTestClient bobGameClient = this.wsClient.connect(GameTestClient.class, String.format("/games/%s/%s", gameId, bobUsername)).blockingFirst();

        // bob joins game
        var fooUsername = "foo";
        GameTestClient fooGameClient = this.wsClient.connect(GameTestClient.class, String.format("/games/%s/%s", gameId, fooUsername)).blockingFirst();

        // alice gets bob and foo join message
        assertThat(aliceGameClient.getSession().isOpen()).isTrue();
        awaitReceivedMessages(aliceGameClient, 2);
        expectPlayerJoined(aliceGameClient, bobUsername);
        expectPlayerJoined(aliceGameClient, fooUsername);

        awaitReceivedMessages(aliceGameClient, 1,3);
        expectGameStarted(aliceGameClient);

        awaitReceivedMessages(aliceGameClient, 1,5);
        expectRoundStarted(aliceGameClient);
    }


    private void expectPlayerJoined(GameTestClient gameClient, String username) {
        var ctx = expectMessageType(gameClient, "player_joined");
        assertThat(ctx.<String>read("$.username")).isEqualTo(username);
    }

    private void expectGameStarted(GameTestClient gameClient) {
        var ctx = expectMessageType(gameClient, "game_started");
    }

    private void expectRoundStarted(GameTestClient gameClient) {
        var ctx = expectMessageType(gameClient, "round_started");
    }

    private ReadContext expectMessageType(GameTestClient gameClient, String messageType) {
        String message = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(message);
        assertThat(ctx.<String>read("$.@type")).isEqualTo(messageType);
        return ctx;
    }

    private void awaitReceivedMessages(GameTestClient gameTestClient, int expected) {
        awaitReceivedMessages(gameTestClient, expected, 1);
    }

    private void awaitReceivedMessages(GameTestClient gameTestClient, int expected, int seconds) {
        Awaitility.await().atMost(Duration.ofSeconds(seconds)).untilAsserted(() -> {
            // joined message + the acks
            assertThat(gameTestClient.getReceived()).hasSizeGreaterThanOrEqualTo(expected);
        });
    }
}
