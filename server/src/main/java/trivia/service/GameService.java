package trivia.service;

import reactor.core.publisher.Mono;
import trivia.domain.Game;

import javax.inject.Singleton;

@Singleton
public class GameService {

    public Mono<Game> createGame(String category) {
        var game = Game.builder()
            .id("100")
            .category(category)
            .build();
        return Mono.just(game);
    }
}
