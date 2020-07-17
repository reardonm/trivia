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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.service.GameService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Slf4j
@ServerWebSocket("/api/games/{gameId}/{username}")
public class WebSocketGameHandler {

    static final MediaType JSON_TYPE = MediaType.APPLICATION_JSON_TYPE;

    private final WebSocketBroadcaster broadcaster;

    private final GameService gameService;

    public WebSocketGameHandler(WebSocketBroadcaster broadcaster, GameService gameService) {
        this.broadcaster = Objects.requireNonNull(broadcaster);
        this.gameService = Objects.requireNonNull(gameService);
    }

    @OnOpen
    public Publisher<GameMessage> onOpen(String gameId, String username, WebSocketSession session) {
        log.info("Open web socket - Game[{}] username[{}]", gameId, username);
        Flux<GameMessage> messages = gameService.joinGame(gameId, username, session.getId())
            .flatMapMany(g -> {
                // build a list of message to send
                List<GameMessage> msgs = new ArrayList<>();

                // player joined the game
                msgs.add(PlayerJoined.builder()
                    .username(username)
                    .playerCount(g.getPlayers())
                    .build());

                // if the game status is started, returned that state
                if (g.isStarted()) {
                    msgs.add(GameStarted.builder().id(gameId).build());

                    // FIXME: stubbing game start for now
                    msgs.add(RoundStarted.builder()
                        .round(1)
                        .question("What is 2 + 2?")
                        .answers(List.of("3","4","Cake"))
                        .build());
                }
                // give the list as a flux
                return Flux.fromIterable(msgs);
            });
        // broad case each of the messages to everyone in the game
        return messages.flatMap(m -> Mono.from(broadcaster.broadcast(m, JSON_TYPE, isGame(gameId))));
    }

    @OnMessage
    public Publisher<GameMessage> onMessage(
        String gameId,
        String username,
        String message,
        WebSocketSession session) {
        log.info("Answer - Game[{}] username[{}]: {}", gameId, username, message);

        Mono<GameMessage> sendToPlayer = gameService.answerQuestion(gameId, username, session.getId())
            .map(b -> b ? PlayerAdvanced.builder().username(username).build() : PlayerEliminated.builder().username(username).build())
            .flatMap(msg -> Mono.from(session.send(msg)));

        // FIXME: stubbing game start for now
        Mono<GameMessage> broadcast = Mono.from(broadcaster.broadcast(
            RoundCompleted.builder()
                .round(1)
                .answer("4")
                .stats(List.of(0,2,1))
                .build(), JSON_TYPE, isGame(gameId)));

        return Flux.concat(sendToPlayer, broadcast);
    }

    @OnClose
    public Publisher<String> onClose(
        String gameId,
        String username,
        WebSocketSession session) {

        log.info("Player left Game: {} msg {}", gameId, username);
        return Mono.empty();
    }

    private Predicate<WebSocketSession> isGame(String gameId) {
        return session ->  gameId.equalsIgnoreCase(session.getUriVariables().get("gameId", String.class, null));
    }
}
