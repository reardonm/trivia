package trivia.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

@Controller("/api")
public class TriviaController {

    @Produces(MediaType.TEXT_PLAIN)
    @Get("/")
    HttpResponse<String> landing() {
        return HttpResponse.ok("Hello world");
    }
}
