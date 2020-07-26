package trivia.service;

import io.micronaut.scheduling.annotation.Scheduled;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.util.Objects;

@Slf4j
@Singleton
public class DefaultGameService implements GameService {

    @Getter
    private final int minPlayers;

    private final GameRepository repository;

    public DefaultGameService(GameRepository gameRepository) {
        this.minPlayers = 3; // TODO: configure
        this.repository = Objects.requireNonNull(gameRepository);
    }

    @Override
    public Mono<Game> createGame(String category) {
        return this.repository.createGame(category)
            .map(id -> Game.builder()
                .id(id)
                .category(category)
                .started(false)
                .players(0)
                .build());
    }

    @Override
    public Mono<Game> joinGame(String gameId, String username, String sessionId) {
        return this.repository.addPlayer(gameId, username);
    }

    @Override
    public Mono<Boolean> answerQuestion(String gameId, String username, String sessionId) {
        throw new UnsupportedOperationException("fix me");
    }

    public Flux<String> subscribeToGameChannel() {
        return repository.subscribeToGameChannel();
    }

    @Scheduled(fixedDelay = "1s", initialDelay = "5s")
    public void pollForGamesWithEnoughPlayers(String channel) {
        repository.startPendingGames(minPlayers);
    }
}
