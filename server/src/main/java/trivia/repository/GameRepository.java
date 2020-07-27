package trivia.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.domain.RoundEvent;

import java.time.Duration;
import java.util.List;


public interface GameRepository {

    Mono<Long> save(Question question);

    /**
     * Find all questions in a category
     * @param category the category to search
     */
    Flux<Question> findQuestionsInCategory(String category);

    /**
     * Find questions in a category
     * @param category the category to search
     * @param n the maximum number to return
     */
    Flux<Question> findQuestionsInCategory(String category, int n);

    /**
     * Find questions in a category, and set the weight of the question so that it's sorted to the bottom so that the
     * same question will not be returned over and over.
     * @param category the category to search
     * @param n the maximum number to return
     */
    Flux<Question> allocateQuestions(String category, int n);

    Flux<String> listCategories();

    /**
     * Greate a game for a give category.
     * @return the id of the game
     */
    Mono<String> createGame(String category, List<Question> questions);

    Mono<Game> findGame(String gameId);

    /**
     * Add a player to the game
     * @return the new total number of players
     */
    Mono<Game> addPlayer(String gameId, String username);

    /**
    * Find pending games with a least the given number of joined players.
     * @param delayStartRound how long between rounds
     * @param minPlayers min number of player needed to start a game
     */
    void startPendingGames(Duration delayStartRound, int minPlayers);

    /**
     * Find pending rounds to start or complete
     * @param delayStartRound how long between rounds
     * @param roundDuration how long a round should s
     */
    void advancePendingRounds(Duration delayStartRound, Duration roundDuration);

    /**
     * Find game question for the given round
     */
    Mono<Question> findQuestionForRound(String gameId, int round);

    void countAnswer(String gameId, int round, String answer);

    Mono<Integer> findPlayerCount(String gameId, int round);

    Flux<String> subscribeToGameChannel();

    Flux<RoundEvent> subscribeToRoundsChannel();

}
