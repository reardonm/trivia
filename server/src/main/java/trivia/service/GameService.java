package trivia.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;

public interface GameService {
    Mono<Game> createGame(String category);

    Mono<Game> joinGame(String gameId, String username, String sessionId);

    Mono<Boolean> answerQuestion(String gameId, String username, String sessionId);

    int getMinPlayers();

    public Flux<String> subscribeToGameChannel();
}
