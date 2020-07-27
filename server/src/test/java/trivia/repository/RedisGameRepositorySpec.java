package trivia.repository;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trivia.IntegerationTestSupport;
import trivia.TestData;
import trivia.domain.Game;
import trivia.domain.Question;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;


class RedisGameRepositorySpec extends IntegerationTestSupport {

    @Inject
    StatefulRedisConnection<String,String> connection;

    @Inject
    StatefulRedisPubSubConnection<String,String> pubSubConnection;

    RedisGameRepository underTest;

    TestData testData;

    @BeforeEach
    public void setUp() {
        // Assume that we have Redis running locally?
        underTest = new RedisGameRepository(connection, pubSubConnection, new JsonEncoder(mapper));
        testData = TestData.load(mapper);
    }

    @Test
    void roundTripQuestionData() throws Exception {
        for (int i = 1; i < 5; i++) {
            assertThat(underTest.save(TestData.createMathQuestion(i)).block()).isGreaterThan(-1);
        }

        List<Question> found = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY).collectList().block();
        assertThat(found).contains(TestData.createMathQuestion(2));
    }

    @Test
    void allocateQuestion() throws Exception {
        List<Question> questions = TestData.createQuestions(10);
        questions.forEach(q -> underTest.save(q).block());

        // take a look at the top question
        Question first = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY, 0).blockFirst();
        assertThat(first).isNotNull();
        String q1 = first.getText();

        // repeat
        first = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY, 0).blockFirst();
        assertThat(first.getText()).isEqualTo(q1); // same result

        // allocate questions
        List<Question> allocated = underTest.allocateQuestions(TestData.MATH_CATEGORY, 2).collectList().block();
        assertThat(allocated).hasSize(3);
        assertThat(allocated.get(0).getText()).isEqualTo(q1); // same result

        // repeat
        first = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY, 0).blockFirst();
        assertThat(first.getText()).isNotEqualTo(q1); // new top question
    }

    @Test
    void listCategories() throws Exception {
        var data = testData.getQuestions();
        for (Question datum : data) {
            underTest.save(datum).block();
        }

        var categories = underTest.listCategories().collectList().block();
        var expected = testData.getCategories().toArray(new String[0]);
        assertThat(categories).contains(expected);
    }

    @Test
    void createGame() throws Exception {
        String title = "Entertainment: Gum";
        String id = stageGame(title);
        assertThat(id).matches(Pattern.compile("\\d+"));

        assertThat(connection.sync().hget(RedisGameRepository.GAME_KEY_PREFIX + id, RedisGameRepository.TITLE))
            .isEqualTo(title);
    }

    @Test
    void findGame() throws Exception {
        String title = "Entertainment: Spam";
        String id = stageGame(title);
        assertThat(id).matches(Pattern.compile("\\d+"));

        Game g = underTest.findGame(id).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getTitle()).isNotBlank();
        assertThat(g.getPlayers()).isEqualTo(0);
        assertThat(g.isStarted()).isFalse();
    }

    @Test
    void findGame_notFound() throws Exception {
        assertThat(underTest.findGame("999-does-not-exist").blockOptional()).isEmpty();
    }

    @Test
    void addPlayerToGame() throws Exception {
        // first create a game
        var title = "Math";
        String gameId = stageGame(title);
        var gameKey = RedisGameRepository.GAME_KEY_PREFIX + gameId;
        var playerKey = RedisGameRepository.PLAYERS_KEY_PREFIX + gameId;

        assertThat(connection.sync().hgetall(gameKey)).containsAllEntriesOf(Map.of(
            RedisGameRepository.TITLE, title
        ));

        // add bob as a player
        assertThat(underTest.addPlayer(gameId, "bob").block()).satisfies(g -> {
            assertThat(g.getId()).isEqualTo(gameId);
            assertThat(g.getPlayers()).isEqualTo(1);
        });
        // add bob as a player
        assertThat(underTest.addPlayer(gameId, "alice").block()).satisfies(g -> {
            assertThat(g.getId()).isEqualTo(gameId);
            assertThat(g.getPlayers()).isEqualTo(2);
        });
        // repeat alice (no change)
        assertThat(underTest.addPlayer(gameId, "alice").blockOptional()).isEmpty();

        // check related data is as expected
        assertThat(underTest.findGame(gameId).block()).satisfies(g -> {
            assertThat(g.getId()).isEqualTo(gameId);
            assertThat(g.getPlayers()).isEqualTo(2);
        });
        assertThat(connection.sync().smembers(playerKey)).contains("bob", "alice");
        assertThat(connection.sync().hgetall(gameKey)).containsAllEntriesOf(Map.of(
            RedisGameRepository.TITLE, title
        ));

        // Game is enqueued to for starting
        assertThat(connection.sync().zscore(RedisGameRepository.GAME_PENDING_KEY, gameId)).isEqualTo(2.0);
    }

    @Test
    void addPlayerToGame_NotFound() throws Exception {
        // repeat alice (no change)
        String doesNotExist = "9999";
        var gameKey = RedisGameRepository.GAME_KEY_PREFIX + doesNotExist;
        var playerKey = RedisGameRepository.PLAYERS_KEY_PREFIX + doesNotExist;

        assertThat(underTest.addPlayer(doesNotExist, "alice").blockOptional()).isEmpty();

        // check related data is as expected
        assertThat(connection.sync().exists(gameKey)).isZero();
        assertThat(connection.sync().exists(playerKey)).isZero();
    }

    @Test
    void addPlayerToGame_AlreadyStarted() throws Exception {
        // first create a game
        var title = "Math";
        String gameId = stageGame(title);
        var gameKey = RedisGameRepository.GAME_KEY_PREFIX + gameId;
        var playerKey = RedisGameRepository.PLAYERS_KEY_PREFIX + gameId;

        // force it to a started state
        connection.sync().hset(gameKey, RedisGameRepository.ROUND, "0");

        assertThat(underTest.addPlayer(gameId, "fred").blockOptional()).isEmpty();

        // check related data is as expected
        assertThat(connection.sync().exists(playerKey)).isZero();
    }



    @Test
    void startPendingGames() throws Exception {
        String g1 = underTest.createGame("Music", List.of()).block();
        String g2 = underTest.createGame("Music", List.of()).block();
        String g3 = underTest.createGame("Music", List.of()).block();

        assertThat(underTest.addPlayer(g1, "alice").block()).isNotNull();
        assertThat(underTest.addPlayer(g1, "bob").block()).isNotNull();
        assertThat(underTest.addPlayer(g1, "bubba").block()).isNotNull();

        assertThat(underTest.addPlayer(g2, "zed").block()).isNotNull();
        assertThat(underTest.addPlayer(g2, "foo").block()).isNotNull();

        assertThat(underTest.addPlayer(g3, "fud").block()).isNotNull();


        // ensure that three games are pending
        assertThat(connection.sync().zrange(RedisGameRepository.GAME_PENDING_KEY, 0, -1))
            .containsOnly(g1, g2, g3);

        underTest.startPendingGames(Duration.ofMillis(100), 2);
        underTest.advancePendingRounds(Duration.ofMillis(100), Duration.ofMillis(200));

        // change the games are all started
        assertThat(underTest.findGame(g1).block()).satisfies(g -> {
            assertThat(g.isStarted()).isTrue();
        });
        assertThat(underTest.findGame(g2).block()).satisfies(g -> {
            assertThat(g.isStarted()).isTrue();
        });
        assertThat(underTest.findGame(g3).block()).satisfies(g -> {
            assertThat(g.isStarted()).isFalse();
        });

        // ensure that only game 3 is still pending
        assertThat(connection.sync().zrange(RedisGameRepository.GAME_PENDING_KEY, 0, -1))
            .containsOnly(g3);
    }

    private String stageGame(String title) {
        List<Question> questions = TestData.createQuestions(5);
        return underTest.createGame(title, questions).block();
    }
}
