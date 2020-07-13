package trivia.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Question;


public interface QuestionRepository {

    Mono<Double> save(Question question);

    Flux<Question> find(String category);

    Flux<String> listCategories();

}
