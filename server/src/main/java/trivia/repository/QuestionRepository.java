package trivia.repository;

import reactor.core.publisher.Mono;
import trivia.domain.Question;

public interface QuestionRepository {

    Mono<Boolean> save(Question question);

    Mono<Question> find(String key);
}
