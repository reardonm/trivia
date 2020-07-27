package trivia.service;

import io.micronaut.scheduling.annotation.Scheduled;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.domain.Round;
import trivia.domain.RoundEvent;
import trivia.repository.GameRepository;

import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    public Mono<Game> createGame(String title, List<Question> questions) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Questions are required");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        return this.repository.createGame(title, questions)
            .map(id -> Game.builder()
                .id(id)
                .title(title)
                .players(0)
                .build());
    }

    @Override
    public Mono<Game> joinGame(String gameId, String username, String sessionId) {
        return this.repository.addPlayer(gameId, username);
    }

    @Override
    public Mono<Round> findRound(String gameId, int round) {
        return this.repository.findQuestionForRound(gameId, round)
            .flatMap(q -> this.repository.findPlayerCount(gameId, round).map(c -> Round.builder()
                .number(round)
                .question(q)
                .players(c)
                .build()));
    }

    @Override
    public Mono<Boolean> answerQuestion(String gameId, String answer) {
        return this.repository.findGame(gameId)
            .flatMap(g -> this.repository.findQuestionForRound(gameId, g.getRound())
                .doOnSuccess(q -> this.repository.countAnswer(gameId, g.getRound(), answer))
                .map(q -> q.getCorrectAnswer().equals(answer)));
    }

    @Override
    public Mono<Map<String, Integer>> findStats(String gameId, int round) {
        throw new UnsupportedOperationException("fix me");
    }

    @Override
    public Flux<String> subscribeToGameChannel() {
        return repository.subscribeToGameChannel();
    }

    @Override
    public Flux<RoundEvent> subscribeRoundMessageEvents() {
        return repository.subscribeToRoundsChannel();
    }

    @Scheduled(fixedDelay = "1s", initialDelay = "5s")
    public void pollForGamesWithEnoughPlayers() {
        Duration roundDuration = Duration.ofSeconds(15);
        Duration delayStartRound = Duration.ofSeconds(3);
        repository.advancePendingRounds(delayStartRound, roundDuration);
        repository.startPendingGames(delayStartRound, minPlayers);
    }
}
