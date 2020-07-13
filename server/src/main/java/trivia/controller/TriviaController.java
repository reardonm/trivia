package trivia.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import reactor.core.publisher.Mono;
import trivia.service.QuestionService;

import java.util.Objects;

@Controller("/api")
public class TriviaController {

    private final QuestionService questionService;

    public TriviaController(QuestionService questionService) {
        this.questionService = Objects.requireNonNull(questionService);
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Get("/categories")
    Mono<CategoriesResponse> categories() {
        return questionService.listCategories().collectList()
            .map(cats -> CategoriesResponse.builder()
                .categories(cats)
                .build());
    }


}
