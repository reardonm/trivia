package trivia.api;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.service.DefaultQuestionService;
import trivia.service.QuestionService;
import trivia.service.GameService;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@MicronautTest
public class TrivaControllerSpec {

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    QuestionService questionService;

    @Inject
    GameService gameService;

    @MockBean(DefaultQuestionService.class)
    QuestionService questionService() {
        return mock(QuestionService.class);
    }

    @MockBean(GameService.class)
    GameService gameService() {
        return mock(GameService.class);
    }


    @Test
    void get_categories() {
        var cats = new String[]{"Math", "Science", "Sports", "Movies"};
        when(questionService.listCategories()).thenReturn(Flux.fromArray(cats));

        var response = client.toBlocking().exchange(HttpRequest.GET("/categories"), String.class);
        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);

        //assert response structure
        ReadContext ctx = JsonPath.parse(response.body());
        List<String> categories = ctx.read("$.categories");
        assertThat(categories).containsExactly(cats);
    }

    @Test
    void post_create_game() {
        String category = "Math";
        when(gameService.createGame(category)).thenReturn(Mono.just(Game.builder()
            .id("100")
            .category(category)
            .build()));

        var response = client.toBlocking().exchange(HttpRequest.POST("/games", Map.of("category", category)), String.class);
        assertThat(response.getStatus().getCode()).isEqualTo(201);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getHeaders().get("Location")).matches(Pattern.compile("/api/games/.+"));

        //assert response structure
        ReadContext ctx = JsonPath.parse(response.body());
        assertThat(ctx.<String>read("$.gameId")).isNotBlank();

        verify(gameService, times(1)).createGame(category);
    }

    @Test
    void post_create_game_invalid() {
        Map<String, String> req = Map.of("category", "");

        assertThrows(HttpClientResponseException.class, () -> {
            var response = client.toBlocking().exchange(HttpRequest.POST("/games", req), String.class);
            assertThat(response.getStatus().getCode()).isEqualTo(400);
            assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
            //assert response structure
            ReadContext ctx = JsonPath.parse(response.body());
            assertThat(ctx.<String>read("$.message")).isEqualTo("request.category: must not be blank");
        });

        verify(gameService, never()).createGame(any(String.class));
    }
}
