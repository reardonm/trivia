package trivia;

import lombok.SneakyThrows;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trivia.domain.Difficulty;
import trivia.domain.Question;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestData {

    public static final String MATH_CATEGORY = "Math";

    public static Question createMathQuestion(int n) {
        return Question.builder()
            .category(MATH_CATEGORY)
            .difficulty(Difficulty.easy)
            .question(String.format("What is %d + %d?", n, n))
            .correctAnswer(String.valueOf(n+n))
            .incorrectAnswers(List.of(String.valueOf(n+1),String.valueOf(n*n),"Donkey"))
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
