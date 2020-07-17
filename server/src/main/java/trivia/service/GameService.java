package trivia.service;

import lombok.Getter;
import reactor.core.publisher.Mono;
import trivia.domain.Game;

import javax.inject.Singleton;

@Singleton
public class GameService {

    @Getter
    private final int minPlayers;

    public GameService() {
        this.minPlayers = 3; // TODO: configure
    }

    public Mono<Game> createGame(String category) {
        var game = Game.builder()
            .id("100")
            .category(category)
            .build();
        return Mono.just(game);
    }

    public Mono<Game> joinGame(String gameId, String username, String sessionId) {
        throw new UnsupportedOperationException("fix me");
    }

    public Mono<Boolean> answerQuestion(String gameId, String username, String sessionId) {
        throw new UnsupportedOperationException("fix me");
    }
}
