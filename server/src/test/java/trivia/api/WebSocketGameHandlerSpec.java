package trivia.api;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.websocket.RxWebSocketClient;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Difficulty;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.service.GameService;

import javax.inject.Inject;
import java.time.Duration;
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
        return mock;
    }

    private final String gameId = "100";
    private final String userName = "alice";
    private final String uri = String.format("/games/%s/%s", gameId, userName);


    @Test
    public void join_game_and_start() throws Exception {
        //  when Alice joins there will be enough players to start the game
        Game game = createGame();
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
        Game game = createGame();
        GameTestClient gameClient = joinGame(game);

        //  when Alice answers a question it will be incorrect
        when(gameService.answerQuestion(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(false));
        var question = broadcastQuestion(gameClient);

        // Alice sends back incorrect answer, gets back a player_eliminated
        gameClient.send(question.getIncorrectAnswers().get(1));
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
        Game game = createGame();
        GameTestClient gameClient = joinGame(game);

        //  when Alice answers a question it will be correct
        when(gameService.answerQuestion(eq(gameId), eq(userName), any(String.class))).thenReturn(Mono.just(true));

        // round started
        var question = broadcastQuestion(gameClient);

        // Alice sends back correct answer, gets back a player_advanced
        gameClient.send(question.getCorrectAnswer());
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
        Game game = createGame();
        GameTestClient gameClient = joinGame(game);
        int round = 3;
        var question = createQuestion();
        gameHandler.broadcastRoundCompleted(gameId, round, question, Map.of(
            question.getCorrectAnswer(), 2,
            question.getIncorrectAnswers().get(0), 0,
            question.getIncorrectAnswers().get(1), 1));

        // round over, stats given
        awaitReceivedMessages(gameClient, 1);
        var msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_completed");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(round);
        assertThat(ctx.<String>read("$.answer")).isEqualTo("4");
        assertThat(ctx.<Map<String,Integer>>read("$.stats")).containsOnly(
            entry(question.getCorrectAnswer(), 2),
            entry(question.getIncorrectAnswers().get(0), 0),
            entry(question.getIncorrectAnswers().get(1), 1));

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

    private Question broadcastQuestion(GameTestClient gameClient) {
        // simulate first round started
        Question question = createQuestion();
        gameHandler.broadcastRoundStarted(gameId, 1, question);
        awaitReceivedMessages(gameClient, 1);
        String msg = gameClient.getReceived().poll();
        ReadContext ctx = JsonPath.parse(msg);
        assertThat(ctx.<String>read("$.@type")).isEqualTo("round_started");
        assertThat(ctx.<Integer>read("$.round")).isEqualTo(1);
        assertThat(ctx.<String>read("$.question")).isEqualTo("What is 2 + 2?");
        assertThat(ctx.<List<String>>read("$.answers")).containsOnly("3", "4", "Cake");
        return question;
    }

    private Question createQuestion() {
        return Question.builder()
                .category("Math")
                .difficulty(Difficulty.easy)
                .question("What is 2 + 2?")
                .correctAnswer("4")
                .incorrectAnswers(List.of("3", "Cake"))
                .build();
    }

    private Game createGame() {
        return Game.builder()
            .id(gameId)
            .category("Math")
            .players(3)
            .started(true)
            .build();
    }

    private void awaitReceivedMessages(GameTestClient gameTestClient, int expected) {
        Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            // joined message + the acks
            assertThat(gameTestClient.getReceived()).hasSize(expected);
        });
    }
}
