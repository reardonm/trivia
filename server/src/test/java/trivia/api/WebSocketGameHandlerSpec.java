package trivia.api;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.websocket.RxWebSocketClient;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.service.GameService;

import javax.inject.Inject;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

    @Inject
    private GameService gameService;

    @MockBean(GameService.class)
    GameService gameService() {
        return mock(GameService.class);
    }

    @Test
    public void join_game_and_start() throws Exception {
        var gameId = "100";
        var userName = "alice";
        var uri = String.format("/games/%s/%s", gameId, userName);
        var game = Game.builder()
            .id(gameId)
            .category("Math")
            .players(3)
            .started(true)
            .build();

        // when Alice joins there will be enough players to start the game
        when(gameService.joinGame(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(game));

        // when Alice answers a question it will be incorrect
        when(gameService.answerQuestion(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(false));

        // Alice joins the game
        GameTestClient gameClient = this.wsClient.connect(GameTestClient.class, uri).blockingFirst();
        assertThat(gameClient.getSession().isOpen()).isTrue();
        assertThat(gameClient.getGameId()).isEqualTo(gameId);
        assertThat(gameClient.getUsername()).isEqualTo(userName);

        // wait for 3 messages
        awaitReceivedMessages(gameClient, 3);

        // first message should be GameStarted
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_joined");
        assertThat(ctx.<String>read("$.username")).isEqualTo(userName);
        assertThat(ctx.<Integer>read("$.playerCount")).isEqualTo(game.getPlayers());

        // first message should be GameStarted
        msg = gameClient.getReceived().poll();
        ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("game_started");
        assertThat(ctx.<String>read("$.id")).isEqualTo(gameId);

        // second message should be Round 1
        msg = gameClient.getReceived().poll();
        ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_started");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(1);
        assertThat(ctx.<String>read("$.question")).isEqualTo("What is 2 + 2?");
        assertThat(ctx.<String>read("$.answers[0]")).isEqualTo("3");
        assertThat(ctx.<String>read("$.answers[1]")).isEqualTo("4");
        assertThat(ctx.<String>read("$.answers[2]")).isEqualTo("Cake");

        // send the wrong answer
        gameClient.send("Cake");

        // wait for 1 messages
        awaitReceivedMessages(gameClient, 2);

        // wrong answer, eliminated
        msg = gameClient.getReceived().poll();
        ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_eliminated");

        // round over
        msg = gameClient.getReceived().poll();
        System.out.println(msg);

        ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_completed");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(1);
        assertThat(ctx.<String>read("$.answer")).isEqualTo("4");
        assertThat(ctx.<Integer>read("$.stats[0]")).isEqualTo(0);
        assertThat(ctx.<Integer>read("$.stats[1]")).isEqualTo(2);
        assertThat(ctx.<Integer>read("$.stats[2]")).isEqualTo(1);

        gameClient.close();
    }


    private void awaitReceivedMessages(GameTestClient gameTestClient, int expected) {
        Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            // joined message + the acks
            assertThat(gameTestClient.getReceived()).hasSize(expected);
        });
    }
}
