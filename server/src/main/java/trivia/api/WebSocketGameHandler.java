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
import reactor.core.publisher.Mono;
import trivia.api.protocol.*;
import trivia.domain.Round;
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

    private Disposable gameMessageEvents;
    private Disposable roundMessageEvents;

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

        this.roundMessageEvents = this.gameService.subscribeRoundMessageEvents()
            .doOnNext(msg -> {
                if (msg.getStarted()) {
                    broadcastRoundStarted(msg.getGameId(), msg.getRound());
                } else {
                    broadcastRoundCompleted(msg.getGameId(), msg.getRound());
                }
            })
            .subscribe();
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        this.gameMessageEvents.dispose();
        this.roundMessageEvents.dispose();
    }

    /**
     * Receive WebSocket open frames which should always be a player joining a game
     */
    @OnOpen
    public Publisher<GameMessage> onOpen(String gameId, String username, WebSocketSession session) {
        log.info("Open web socket - Game[{}] username[{}]", gameId, username);
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
        return gameService.answerQuestion(gameId, message)
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
        log.info("Player[{}] left Game[{}]", username, gameId);
        return Mono.empty();
    }

    void broadcastGameStarted(String gameId) {
        var msg = GameStarted.builder().id(gameId).build();
        log.info("Game[{}] started", gameId);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    void broadcastRoundStarted(String gameId, int roundNumber) {
        // shuffle correct and incorrect answers
        Round round = gameService.findRound(gameId, roundNumber).blockOptional().orElseThrow(); // FIXME
        // move to service
        var answers = new ArrayList<>(round.getQuestion().getIncorrectAnswers());
        answers.add(round.getQuestion().getCorrectAnswer());
        Collections.shuffle(answers);
        var msg = RoundStarted.builder()
            .round(roundNumber)
            .question(round.getQuestion().getText())
            .answers(answers)
            .build();
        log.info("Game[{}] Round[{}] started", gameId, round);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    void broadcastRoundCompleted(String gameId, int roundNumber) {
        Round round = gameService.findRound(gameId, roundNumber).blockOptional().orElseThrow(); // FIXME
        Map<String,Integer> stats = gameService.findStats(gameId, roundNumber).blockOptional().orElseThrow(); // FIXME
        var msg = RoundCompleted.builder()
            .round(roundNumber)
            .answer(round.getQuestion().getCorrectAnswer())
            .stats(stats)
            .players(round.getPlayers())
            .build();
        log.info("Game[{}] Round[{}] completed", gameId, round);
        broadcaster.broadcastSync(msg, JSON_TYPE, isGame(gameId));
    }

    private Predicate<WebSocketSession> isGame(String gameId) {
        return session ->  gameId.equalsIgnoreCase(session.getUriVariables().get("gameId", String.class, null));
    }
}
