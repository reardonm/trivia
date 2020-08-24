package trivia.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.TriviaConfig;
import trivia.domain.Question;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;

@Singleton
public class DefaultQuestionService implements QuestionService {

    private final GameRepository repository;
    private final int numRounds;

    public DefaultQuestionService(GameRepository repository, TriviaConfig config) {
        this.repository = Objects.requireNonNull(repository);
        this.numRounds = config.getRoundsPerGame();
    }

    @Override
    public Flux<String> listCategories() {
        return repository.listCategories();
    }

    @Override
    public Mono<List<Question>> allocateQuestions(String category) {
        return repository.allocateQuestions(category, numRounds).collectList()
            .filter(qs -> qs.size() == numRounds)
            .switchIfEmpty(Mono.error(() -> new InsufficientDataException("Not enough questions to allocate " + numRounds)));
    }
}
