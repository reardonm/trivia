package trivia.service;

import reactor.core.publisher.Flux;

public interface QuestionService {

    Flux<String> listCategories();
}
