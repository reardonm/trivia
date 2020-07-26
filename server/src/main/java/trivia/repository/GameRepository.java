package trivia.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public interface GameRepository {

    Mono<Double> save(Question question);

    Flux<Question> findQuestionsInCategory(String category);

    Flux<String> listCategories();

    /**
     * Greate a game for a give category.
     * @return the id of the game
     */
    Mono<String> createGame(String category);

    Mono<Game> findGame(String gameId);

    /**
     * Add a player to the game
     * @return the new total number of players
     */
    Mono<Game> addPlayer(String gameId, String username);

    /**
    * Find pending games with a least the given number of joined players.
     * @param minPlayers min number of player needed to start a game
     */
    void startPendingGames(int minPlayers);

    Flux<String> subscribeToGameChannel();
}
