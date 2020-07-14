package trivia.api;


import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Collection<String> received = new ConcurrentLinkedQueue<>();

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
