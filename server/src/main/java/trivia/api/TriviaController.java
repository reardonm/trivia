package trivia.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.validation.Validated;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.service.GameService;
import trivia.service.QuestionService;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Controller("/api")
@Validated
public class TriviaController {

    private final QuestionService questionService;
    private final GameService gameService;

    public TriviaController(QuestionService questionService, GameService gameService) {
        this.questionService = Objects.requireNonNull(questionService);
        this.gameService = Objects.requireNonNull(gameService);
    }

    @Get("/categories")
    Mono<CategoriesResponse> categories() {
        return questionService.listCategories()
            .collectList()
            .map(this::createCategoryResponse);
    }

    @Post("/games")
    Mono<HttpResponse<CreateGameResponse>> createGame(@Body @Valid CreateGameRequest request) {
        return gameService.createGame(request.getCategory())
            .map(game -> HttpResponse.created(createCreateGameResponse(game.getId()), buildGameUri(game)));
    }

    private URI buildGameUri(Game game) {
        return UriBuilder.of("/api/games").path(game.getId()).build();
    }

    private CreateGameResponse createCreateGameResponse(String id) {
        var resp = new CreateGameResponse();
        resp.setGameId(id);
        return resp;
    }

    private CategoriesResponse createCategoryResponse(List<String> cats) {
        var resp = new CategoriesResponse();
        resp.setCategories(cats);
        return resp;
    }
}
