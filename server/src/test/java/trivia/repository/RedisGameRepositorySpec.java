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
        underTest = new RedisGameRepository(connection, pubSubConnection, mapper);
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
    void incrementQuestionWeight() throws Exception {
        var q8 = TestData.createMathQuestion(8);
        assertThat(underTest.save(q8).block()).isEqualTo(1.0);
        var q9 = TestData.createMathQuestion(9);
        assertThat(underTest.save(q9).block()).isEqualTo(1.0);

        List<Question> found = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY).collectList().block();
        assertThat(found).containsExactly(q8, q9);

        // save q3 again which would increment the weight
        assertThat(underTest.save(q8).block()).isEqualTo(2.0);
        assertThat(underTest.save(q8).block()).isEqualTo(3.0);
        assertThat(underTest.save(q8).block()).isEqualTo(4.0);

        found = underTest.findQuestionsInCategory(TestData.MATH_CATEGORY).collectList().block();
        assertThat(found).containsExactly(q9, q8);
    }

    @Test
    void listCategories() throws Exception {
        var data = testData.getQuestions();
        for (Question datum : data) {
            assertThat(underTest.save(datum).block()).isEqualTo(1);
        }

        var categories = underTest.listCategories().collectList().block();
        var expected = testData.getCategories().toArray(new String[0]);
        assertThat(categories).contains(expected);
    }

    @Test
    void createGame() throws Exception {
        String category = "Entertainment: Gum";
        var id = underTest.createGame(category).block();
        assertThat(id).matches(Pattern.compile("\\d+"));

        assertThat(connection.sync().hget(RedisGameRepository.GAME_KEY_PREFIX + id, "category"))
            .isEqualTo(category);
    }

    @Test
    void findGame() throws Exception {
        String category = "Entertainment: Spam";
        var id = underTest.createGame(category).block();
        assertThat(id).matches(Pattern.compile("\\d+"));

        Game g = underTest.findGame(id).block();
        assertThat(g).isNotNull();
        assertThat(g.getId()).isNotBlank();
        assertThat(g.getCategory()).isNotBlank();
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
        var category = "Math";
        var gameId = underTest.createGame(category).block();
        var gameKey = RedisGameRepository.GAME_KEY_PREFIX + gameId;
        var playerKey = RedisGameRepository.PLAYERS_KEY_PREFIX + gameId;

        assertThat(connection.sync().hgetall(gameKey)).containsAllEntriesOf(Map.of(
            RedisGameRepository.CATEGORY, category
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
            RedisGameRepository.CATEGORY, category
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
        var category = "Math";
        var gameId = underTest.createGame(category).block();
        var gameKey = RedisGameRepository.GAME_KEY_PREFIX + gameId;
        var playerKey = RedisGameRepository.PLAYERS_KEY_PREFIX + gameId;

        // force it to a started state
        connection.sync().hset(gameKey, RedisGameRepository.STARTED, "true");

        assertThat(underTest.addPlayer(gameId, "fred").blockOptional()).isEmpty();

        // check related data is as expected
        assertThat(connection.sync().exists(playerKey)).isZero();
    }

    @Test
    void startPendingGames() throws Exception {
        String g1 = underTest.createGame("Music").block();
        String g2 = underTest.createGame("Music").block();
        String g3 = underTest.createGame("Music").block();

        assertThat(underTest.addPlayer(g1, "alice").block()).isNotNull();
        assertThat(underTest.addPlayer(g1, "bob").block()).isNotNull();
        assertThat(underTest.addPlayer(g1, "bubba").block()).isNotNull();

        assertThat(underTest.addPlayer(g2, "zed").block()).isNotNull();
        assertThat(underTest.addPlayer(g2, "foo").block()).isNotNull();

        assertThat(underTest.addPlayer(g3, "fud").block()).isNotNull();


        // ensure that three games are pending
        assertThat(connection.sync().zrange(RedisGameRepository.GAME_PENDING_KEY, 0, -1))
            .containsOnly(g1, g2, g3);

        underTest.startPendingGames(2);

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
}
