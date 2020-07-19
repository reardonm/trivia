package trivia.service;

import reactor.core.publisher.Flux;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class DefaultQuestionService implements QuestionService {

    private final GameRepository repository;

    public DefaultQuestionService(GameRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Flux<String> listCategories() {
        return repository.listCategories();
    }
}
