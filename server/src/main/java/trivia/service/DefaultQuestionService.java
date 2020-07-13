package trivia.service;

import reactor.core.publisher.Flux;
import trivia.repository.QuestionRepository;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class DefaultQuestionService implements QuestionService {

    private final QuestionRepository repository;

    public DefaultQuestionService(QuestionRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Flux<String> listCategories() {
        return repository.listCategories();
    }
}
