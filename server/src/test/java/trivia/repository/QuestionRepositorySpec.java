package trivia.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trivia.IntegerationTestSupport;
import trivia.TestData;
import trivia.domain.Question;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;


class QuestionRepositorySpec extends IntegerationTestSupport {

    RedisQuestionRepository underTest;

    TestData testData;

    @BeforeEach
    public void setUp() {
        // Assume that we have Redis running locally?
        underTest = new RedisQuestionRepository(connection, mapper);
        testData = TestData.load(mapper);
    }

    @Test
    void roundTripData() throws Exception {
        for (int i = 1; i < 5; i++) {
            assertThat(underTest.save(TestData.createMathQuestion(i)).block()).isGreaterThan(-1);
        }

        List<Question> found = underTest.find(TestData.MATH_CATEGORY).collectList().block();
        assertThat(found).contains(TestData.createMathQuestion(2));
    }

    @Test
    void incrementQuestionWeight() throws Exception {
        var q8 = TestData.createMathQuestion(8);
        assertThat(underTest.save(q8).block()).isEqualTo(1.0);
        var q9 = TestData.createMathQuestion(9);
        assertThat(underTest.save(q9).block()).isEqualTo(1.0);

        List<Question> found = underTest.find(TestData.MATH_CATEGORY).collectList().block();
        assertThat(found).containsExactly(q8, q9);

        // save q3 again which would increment the weight
        assertThat(underTest.save(q8).block()).isEqualTo(2.0);
        assertThat(underTest.save(q8).block()).isEqualTo(3.0);
        assertThat(underTest.save(q8).block()).isEqualTo(4.0);

        found = underTest.find(TestData.MATH_CATEGORY).collectList().block();
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
}
