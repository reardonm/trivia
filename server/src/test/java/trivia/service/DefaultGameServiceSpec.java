package trivia.service;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import trivia.TestData;
import trivia.TriviaConfig;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.repository.GameRepository;

import javax.inject.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        TriviaConfig config = new TriviaConfig();
        config.setMinimumPlayersPerGame(3);
        service = new DefaultGameService(repository, config);
    }

    @Test
    void createGame() {
        String category = "Math";
        List<Question> qs = TestData.createQuestions(3);
        when(repository.createGame(category, qs)).thenReturn(Mono.just("100"));

        Game g = service.createGame(category, qs).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getTitle()).isEqualTo(category);
        assertThat(g.getPlayers()).isEqualTo(0);
        assertThat(g.isStarted()).isFalse();

        verify(repository, times(1)).createGame(category, qs);
    }

    @Test
    void createGame_WithoutQuestions() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createGame("Foo", List.of()).block();
        });

        assertThat(ex.getMessage()).isEqualTo("Questions are required");

        verify(repository, times(0)).createGame(anyString(), anyList());
    }

    @Test
    void createGame_WithoutTitle() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            service.createGame("", TestData.createQuestions(1)).block();
        });

        assertThat(ex.getMessage()).isEqualTo("Title must not be blank");

        verify(repository, times(0)).createGame(anyString(), anyList());
    }

    @Test
    void joinGame() {
        String gameId = "100";
        String username = "bob";
        String sessionId = "aSessionId";

        when(repository.addPlayer(gameId, username)).thenReturn(Mono.just(Game.builder()
            .id(gameId)
            .title("Math")
            .players(3)
            .build()));

        Game g = service.joinGame(gameId, username, sessionId).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getTitle()).isNotBlank();
        assertThat(g.getPlayers()).isEqualTo(3);
        assertThat(g.isStarted()).isFalse();

        verify(repository, times(1)).addPlayer(gameId, username);
    }
}
