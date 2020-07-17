package trivia.service;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trivia.domain.Game;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
public class GameServiceSpec {

    GameService service;

    @BeforeEach
    void setup() {
        service = new GameService();
    }

    @Test
    void createGame() {
        String category = "Math";
        Game g = service.createGame(category).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getCategory()).isEqualTo(category);
    }
}
