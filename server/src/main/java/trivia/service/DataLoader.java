package trivia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.discovery.event.ServiceReadyEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import trivia.TriviaConfig;
import trivia.domain.Question;
import trivia.repository.RedisGameRepository;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Singleton
@Requires(notEnv = Environment.TEST)
public class DataLoader implements ApplicationEventListener<ServiceReadyEvent> {

    private final RedisGameRepository repository;
    private final ObjectMapper mapper;
    private final Path dataPath;

    public DataLoader(ObjectMapper mapper, RedisGameRepository repository, TriviaConfig config) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
        this.dataPath = Path.of(config.getDataPath());
    }

    @Async
    @Override
    public void onApplicationEvent(final ServiceReadyEvent event) {
        if (this.repository.listCategories().single().blockOptional().isEmpty()) {
            log.info("Loading data at startup from {}", this.dataPath.toAbsolutePath());
            loadQuestions().flatMap(repository::save)
                .subscribe(
                    q -> log.debug("Saved question {}.", q),
                    throwable -> log.error("Error saving question.", throwable));
        } else {
            log.info("Existing question data found. No data will be loaded");
        }
    }

    private Flux<Question> loadQuestions() {
        if (Files.isDirectory(this.dataPath)) {
            return Flux.fromStream(loadJsonFiles())
                .flatMap(f -> Flux.fromIterable(parseQuestions(f)));
        } else {
            return Flux.fromIterable(parseQuestions(this.dataPath));
        }
    }

    private Stream<Path> loadJsonFiles() {
        try {
            return Files.list(this.dataPath)
                .filter(p -> p.getFileName().endsWith(".json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load question data", e);
        }
    }

    private List<Question> parseQuestions(Path questionData) {
        try (InputStream is = Files.newInputStream(questionData)) {
            return mapper.readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Cannot load question data " + questionData, e);
        }
    }
}
