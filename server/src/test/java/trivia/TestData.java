package trivia;

import lombok.SneakyThrows;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trivia.domain.Difficulty;
import trivia.domain.Game;
import trivia.domain.Question;
import trivia.domain.Round;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestData {

    public static final String MATH_CATEGORY = "Math";

    public static Question createMathQuestion(int n) {
        return Question.builder()
            .category(MATH_CATEGORY)
            .difficulty(Difficulty.easy)
            .text(String.format("What is %d + %d?", n, n))
            .correctAnswer(String.valueOf(n+n))
            .incorrectAnswers(List.of(String.valueOf(n+1),String.valueOf(n * n + 1),"Donkey"))
            .build();
    }

    public static List<Question> createQuestions(int n) {
        return IntStream.range(0, n)
            .mapToObj(i -> createMathQuestion(i + 1))
            .collect(Collectors.toList());
    }

    public static Game createGame(String gameId) {
        return Game.builder()
            .id(gameId)
            .title("Math")
            .players(3)
            .build();
    }

    public static Round createRound(int roundNumber) {
        return Round.builder()
            .number(roundNumber)
            .question(TestData.createMathQuestion(3))
            .players(3)
            .build();
    }

    @SneakyThrows
    public static TestData load(ObjectMapper mapper)  {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try(InputStream is = classloader.getResourceAsStream("questions.json")) {
            assertThat(is).isNotNull();
            List<Question> questions = mapper.readValue(is, new TypeReference<>() {});
            return new TestData(questions);
        }
    }


    private final List<Question> questions;

    private TestData(List<Question> questions){
        this.questions = Collections.unmodifiableList(questions);
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Collection<String> getCategories() {
        return questions.stream().map(Question::getCategory).collect(Collectors.toSet());
    }
}
