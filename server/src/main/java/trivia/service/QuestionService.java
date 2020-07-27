package trivia.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Question;

import java.util.List;

public interface QuestionService {

    Flux<String> listCategories();

    /**
     * Allocate random questions from the given category
     * @param category the category to allocate questions from
     * @param n the number of questions to give
     * @return the questions
     */
    Mono<List<Question>> allocateQuestions(String category, int n);
}
