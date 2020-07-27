package trivia.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Question;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;

@Singleton
public class DefaultQuestionService implements QuestionService {

    private final GameRepository repository;

    public DefaultQuestionService(GameRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Flux<String> listCategories() {
        return repository.listCategories();
    }

    @Override
    public Mono<List<Question>> allocateQuestions(String category, int n) {
        return repository.allocateQuestions(category, n).collectList()
            .filter(qs -> qs.size() == n)
            .switchIfEmpty(Mono.error(() -> new InsufficientDataException("Not enough questions to allocate " + n)));
    }
}
