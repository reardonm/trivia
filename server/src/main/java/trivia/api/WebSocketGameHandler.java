package trivia.api;

import io.micronaut.http.MediaType;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.api.protocol.*;
import trivia.domain.Question;
import trivia.service.GameService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@ServerWebSocket("/api/games/{gameId}/{username}")
public class WebSocketGameHandler implements AutoCloseable {

    static final MediaType JSON_TYPE = MediaType.APPLICATION_JSON_TYPE;

    private final WebSocketBroadcaster broadcaster;

    private final GameService gameService;

    private final Map<String, WebSocketSession> sessions = new HashMap<>();

    private Disposable gameMessageEvents;

    public WebSocketGameHandler(WebSocketBroadcaster broadcaster, GameService gameService) {
        this.broadcaster = Objects.requireNonNull(broadcaster);
        this.gameService = Objects.requireNonNull(gameService);
    }

    @PostConstruct
    public void initialize() {
        // listen to game start events
        this.gameMessageEvents = this.gameService.subscribeToGameChannel()
            .doOnNext(this::broadcastGameStarted)
            .subscribe();
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        this.gameMessageEvents.dispose();
    }

    /**
     * Receive WebSocket open frames which should always be a player joining a game
     */
    @OnOpen
    public Publisher<GameMessage> onOpen(String gameId, String username, WebSocketSession session) {
        log.info("Open web socket - Game[{}] username[{}]", gameId, username);
        this.sessions.putIfAbsent(username, session);
        Mono<GameMessage> message = gameService.joinGame(gameId, username, session.getId())
            .map(g -> PlayerJoined.builder()
                    .username(username)
                    .playerCount(g.getPlayers())
                    .build());

        // broadcast to everyone in the game
        return message.flatMap(m -> Mono.from(broadcaster.broadcast(m, JSON_TYPE, isGame(gameId))));
    }

    /**
     * Receive WebSocket message frames. This will be answers to questions
     */
    @OnMessage
    public Publisher<GameMessage> onMessage(
        String gameId,
        String username,
        String message,
        WebSocketSession session) {
        log.info("Answer - Game[{}] username[{}]: {}", gameId, username, message);

        // if correct answer, send PlayerAdvanced message, otherwise send PlayerEliminated message.
        return gameService.answerQuestion(gameId, username, session.getId())
            .map(b -> b ? PlayerAdvanced.builder().username(username).build() : PlayerEliminated.builder().username(username).build())
            .flatMap(msg -> Mono.from(session.send(msg))); // not broadcast
    }

    /**
     *  Receive WebSocket close frames. This will players leaving the game
     */
    @OnClose
    public Publisher<String> onClose(
        String gameId,
        String username,
        WebSocketSession session) {
        sessions.remove(username);
        log.info("Player[{}] left Game[{}]", username, gameId);
        return Mono.empty();
    }

    void broadcastGameStarted(String gameId) {
        var msg = GameStarted.builder().id(gameId).build();
        log.info("Game[{}] started", gameId);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    void broadcastRoundStarted(String gameId, int round, Question question) {
        // shuffle correct and incorrect answers
        var answers = new ArrayList<>(question.getIncorrectAnswers());
        answers.add(question.getCorrectAnswer());
        Collections.shuffle(answers);
        var msg = RoundStarted.builder()
            .round(round)
            .question(question.getQuestion())
            .answers(answers)
            .build();
        log.info("Game[{}] Round[{}] started", gameId, round);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    void broadcastRoundCompleted(String gameId, int round, Question question, Map<String,Integer> stats) {
        var msg = RoundCompleted.builder()
            .round(round)
            .answer(question.getCorrectAnswer())
            .stats(stats)
            .build();
        log.info("Game[{}] Round[{}] completed", gameId, round);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    private Predicate<WebSocketSession> isGame(String gameId) {
        return session ->  gameId.equalsIgnoreCase(session.getUriVariables().get("gameId", String.class, null));
    }
}
