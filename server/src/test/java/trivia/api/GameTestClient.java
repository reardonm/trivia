package trivia.api;


import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Websocket client for testing. The ClientWebSocket annotation instructs Micronaut to produce an implementation at compile time.
 */
@Slf4j
@ClientWebSocket("/games/{gameId}/{username}")
public abstract class GameTestClient implements AutoCloseable {

    @Getter
    private WebSocketSession session;

    @Getter
    private HttpRequest<?> request;

    @Getter
    private String gameId;

    @Getter
    private String username;

    @Getter
    private final Queue<String> received = new ConcurrentLinkedQueue<>();

    public void clearReceived() {
        received.clear();
    }

    @OnOpen
    public void onOpen(String gameId, String username, WebSocketSession session, HttpRequest<?> request) {
        this.gameId = gameId;
        this.username = username;
        this.session = session;
        this.request = request;
    }

    @OnMessage
    public void onMessage(String message) {
        received.add(message);
    }

    public abstract void send(String message);

    @Override
    public void close() {
        session.close();
        log.info("Close");
    }
}
