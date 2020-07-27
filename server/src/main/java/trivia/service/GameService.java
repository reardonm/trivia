package trivia.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.domain.Round;
import trivia.domain.RoundEvent;

import java.util.List;
import java.util.Map;

public interface GameService {

    int getMinPlayers();

    Mono<Game> createGame(String title, List<Question> questions);

    Mono<Game> joinGame(String gameId, String username, String sessionId);

    Mono<Round> findRound(String gameId, int round);

    Mono<Boolean> answerQuestion(String gameId, String answer);

    Mono<Map<String, Integer>> findStats(String gameId, int round);

    Flux<String> subscribeToGameChannel();

    Flux<RoundEvent> subscribeRoundMessageEvents();

}
