package trivia.service;

import lombok.Getter;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.util.Objects;

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
        return this.repository.addPlayer(gameId, username)
            .flatMap(i -> {
                if (i == 0) {
                    return Mono.empty();
                } else {
                    return this.repository.findGame(gameId);
                }
            });
    }

    @Override
    public Mono<Boolean> answerQuestion(String gameId, String username, String sessionId) {
        throw new UnsupportedOperationException("fix me");
    }
}
