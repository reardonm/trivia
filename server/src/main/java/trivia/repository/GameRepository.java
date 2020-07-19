package trivia.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;


public interface GameRepository {

    Mono<Double> save(Question question);

    Flux<Question> findQuestionsInCategory(String category);

    Flux<String> listCategories();

    /**
     * Greate a game for a give category.
     * @return the id of the game
     */
    Mono<String> createGame(String category);

    /**
     * Add a player to the game
     * @return the new total number of players
     */
    Mono<Long> addPlayer(String gameId, String username);

    Mono<Game> findGame(String gameId);
}
