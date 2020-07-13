package trivia.controller;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import trivia.service.DefaultQuestionService;
import trivia.service.QuestionService;

import javax.inject.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@MicronautTest(packages="trivia.controller")
class TrivaControllerSpec {

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    QuestionService questionService;

    @MockBean(DefaultQuestionService.class)
    QuestionService questionService() {
        return mock(QuestionService.class);
    }

    @Test
    void get_categories() {
        var cats = new String[]{"Math", "Science", "Sports", "Movies"};
        when(questionService.listCategories()).thenReturn(Flux.fromArray(cats));

        var response = client.toBlocking().exchange(HttpRequest.GET("/categories"), String.class);
        assertThat(response.getStatus().getCode()).isEqualTo(200);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        ReadContext ctx = JsonPath.parse(response.body());
        List<String> categories = ctx.read("$.categories");

        assertThat(categories).containsExactly(cats);
    }
}
