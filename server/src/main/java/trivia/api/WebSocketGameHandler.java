package trivia.api;

import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import trivia.domain.Question;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Slf4j
@ServerWebSocket("/api/games/{gameId}/{username}")
public class WebSocketGameHandler {

    private final WebSocketBroadcaster broadcaster;

    public WebSocketGameHandler(WebSocketBroadcaster broadcaster) {
        this.broadcaster = Objects.requireNonNull(broadcaster);
    }

    @OnOpen
    public Publisher<String> onOpen(String gameId, String username, WebSocketSession session) {
        String msg = ">>> [" + username + "] joined";
        log.info("onOpen Game : {} msg {}", gameId, msg);
        return broadcaster.broadcast(msg, isValid(gameId));
    }

    @OnMessage
    public Publisher<List<String>> onMessage(
        String gameId,
        String username,
        String message,
        WebSocketSession session) {

        //String msg = ">>> [" + username + "] " + message;
        //log.info("onMessage Game : {} msg {}", gameId, msg);

        var msg = List.of(username, "foo","bar", "message");
        return broadcaster.broadcast(msg, isValid(gameId));
    }

    @OnClose
    public Publisher<String> onClose(
        String gameId,
        String username,
        WebSocketSession session) {

        String msg = ">>> [" + username + "] disconnected";
        log.info("onClose Game: {} msg {}", gameId, msg);
        return broadcaster.broadcast(msg, isValid(gameId));
    }

    private Predicate<WebSocketSession> isValid(String gameId) {
        return s -> true;
        //return s -> gameId.equalsIgnoreCase(s.getUriVariables().get("gameId", String.class, null));
    }
}
