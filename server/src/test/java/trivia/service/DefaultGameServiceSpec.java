package trivia.service;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import trivia.api.WebSocketGameHandler;
import trivia.domain.Game;
import trivia.repository.GameRepository;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@MicronautTest
public class DefaultGameServiceSpec {

    @Inject
    GameRepository repository;

    @Inject
    WebSocketBroadcaster broadcaster;

    GameService service;

    @MockBean(GameRepository.class)
    GameRepository repository() {
        return mock(GameRepository.class);
    }

    @MockBean(WebSocketBroadcaster.class)
    WebSocketBroadcaster broadcaster() {
        return mock(WebSocketBroadcaster.class);
    }

    @BeforeEach
    void setup() {
        service = new DefaultGameService(repository);
    }

    @Test
    void createGame() {
        String category = "Math";

        when(repository.createGame(category)).thenReturn(Mono.just("100"));

        Game g = service.createGame(category).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getCategory()).isEqualTo(category);
        assertThat(g.getPlayers()).isEqualTo(0);
        assertThat(g.isStarted()).isFalse();

        verify(repository, times(1)).createGame(category);
    }

    @Test
    void joinGame() {
        String gameId = "100";
        String username = "bob";
        String sessionId = "aSessionId";

        when(repository.addPlayer(gameId, username)).thenReturn(Mono.just(Game.builder()
            .id(gameId)
            .category("Math")
            .players(3)
            .started(false)
            .build()));

        Game g = service.joinGame(gameId, username, sessionId).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getCategory()).isNotBlank();
        assertThat(g.getPlayers()).isEqualTo(3);
        assertThat(g.isStarted()).isFalse();

        verify(repository, times(1)).addPlayer(gameId, username);
    }
}
