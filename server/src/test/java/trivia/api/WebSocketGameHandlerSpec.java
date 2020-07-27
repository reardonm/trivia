package trivia.api;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.websocket.RxWebSocketClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.TestData;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.service.GameService;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
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
    private WebSocketGameHandler gameHandler;

    @Inject
    private GameService gameService;

    @MockBean(GameService.class)
    GameService gameService() {
        GameService mock = mock(GameService.class);
        when(mock.subscribeToGameChannel()).thenReturn(Flux.empty());
        when(mock.subscribeRoundMessageEvents()).thenReturn(Flux.empty());
        return mock;
    }

    private final String gameId = "100";
    private final String userName = "alice";
    private final String uri = String.format("/games/%s/%s", gameId, userName);
    private final Game game = TestData.createGame(gameId);

    @Test
    public void join_game_and_start() throws Exception {
        //  when Alice joins there will be enough players to start the game
        when(gameService.joinGame(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(game));

        //  Alice joins the game
        GameTestClient gameClient = this.wsClient.connect(GameTestClient.class, uri).blockingFirst();
        assertThat(gameClient.getSession().isOpen()).isTrue();
        assertThat(gameClient.getGameId()).isEqualTo(gameId);
        assertThat(gameClient.getUsername()).isEqualTo(userName);

        // wait for first message - should be player_joined
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_joined");
        assertThat(ctx.<String>read("$.username")).isEqualTo(userName);
        assertThat(ctx.<Integer>read("$.playerCount")).isEqualTo(game.getPlayers());

        // simulate game started
        gameHandler.broadcastGameStarted(gameId);
        // Alice should receive a game_started message
        awaitReceivedMessages(gameClient, 1);
        msg = gameClient.getReceived().poll();
        ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("game_started");
        assertThat(ctx.<String>read("$.id")).isEqualTo(gameId);

        gameClient.close();
    }

    @Test
    public void player_eliminated_for_incorrect_answer() throws Exception {
        //  Given Alice has joined a game
        GameTestClient gameClient = joinGame(game);

        var round = TestData.createRound(0);
        when(gameService.findRound(eq(gameId), eq(0))).thenReturn(Mono.just(round));

        //  when Alice answers a question it will be incorrect
        when(gameService.answerQuestion(eq(gameId), any(String.class))).thenReturn(Mono.just(false));

        broadcastQuestion(gameClient, round.getQuestion());

        // Alice sends back incorrect answer, gets back a player_eliminated
        gameClient.send(round.getQuestion().getIncorrectAnswers().get(1));
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_eliminated");
        assertThat(ctx.<String>read("$.username")).isEqualTo(userName);

        gameClient.close();
    }

    @Test
    public void player_advances_for_correct_answer() throws Exception {
        //  Given Alice has joined a game
        GameTestClient gameClient = joinGame(game);

        var round = TestData.createRound(0);
        when(gameService.findRound(eq(gameId), eq(0))).thenReturn(Mono.just(round));

        //  when Alice answers a question it will be correct
        when(gameService.answerQuestion(eq(gameId), any(String.class))).thenReturn(Mono.just(true));

        // round started
        broadcastQuestion(gameClient, round.getQuestion());

        // Alice sends back correct answer, gets back a player_advanced
        gameClient.send(round.getQuestion().getCorrectAnswer());
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_advanced");
        assertThat(ctx.<String>read("$.username")).isEqualTo(userName);

        gameClient.close();
    }

    @Test
    public void stats_broadcast_when_round_is_completed() throws Exception {
        //  Given Alice has joined a game
        GameTestClient gameClient = joinGame(game);
        var round = TestData.createRound(2);
        when(gameService.findRound(eq(gameId), eq(round.getNumber()))).thenReturn(Mono.just(round));

        Map<String, Integer> stats = Map.of(
            round.getQuestion().getCorrectAnswer(), 2,
            round.getQuestion().getIncorrectAnswers().get(0), 0,
            round.getQuestion().getIncorrectAnswers().get(1), 1);
        when(gameService.findStats(eq(gameId), eq(round.getNumber()))).thenReturn(Mono.just(stats));

        gameHandler.broadcastRoundCompleted(gameId, round.getNumber());

        // round over, stats given
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_completed");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(round.getNumber());
        assertThat(ctx.<String>read("$.answer")).isEqualTo(round.getQuestion().getCorrectAnswer());
        assertThat(ctx.<Map<String,Integer>>read("$.stats")).containsOnly(
            entry(round.getQuestion().getCorrectAnswer(), 2),
            entry(round.getQuestion().getIncorrectAnswers().get(0), 0),
            entry(round.getQuestion().getIncorrectAnswers().get(1), 1));

        gameClient.close();
    }


    private GameTestClient joinGame(Game game) {
        when(gameService.joinGame(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(game));
        //  Alice joins the game
        GameTestClient gameClient = this.wsClient.connect(GameTestClient.class, uri).blockingFirst();
        assertThat(gameClient.getSession().isOpen()).isTrue();
        // wait for first message - should be player_joined
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("player_joined");
        return gameClient;
    }

    private Question broadcastQuestion(GameTestClient gameClient, Question question) {
        // simulate first round started
        gameHandler.broadcastRoundStarted(gameId, 0);
        awaitReceivedMessages(gameClient, 1);
        String msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_started");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(0);
        assertThat(ctx.<String>read("$.question")).isEqualTo(question.getText());

        var answers = new ArrayList<>(question.getIncorrectAnswers());
        answers.add(question.getCorrectAnswer());
        assertThat(ctx.<List<String>>read("$.answers")).containsOnly(answers.toArray(new String[0]));
        return question;
    }

    private void awaitReceivedMessages(GameTestClient gameTestClient, int expected) {
        Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            // joined message + the acks
            assertThat(gameTestClient.getReceived()).hasSize(expected);
        });
    }
}
