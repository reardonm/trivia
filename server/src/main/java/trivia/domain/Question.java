package trivia.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonDeserialize(builder = Question.QuestionBuilder.class)
@Value
@Builder
public class Question {

    private String category;

    private Difficulty difficulty;

    @JsonProperty("question")
    private String text;

    @JsonProperty("correct_answer")
    private String correctAnswer;

    @JsonProperty("incorrect_answers")
    private List<String> incorrectAnswers;

    @JsonPOJOBuilder(withPrefix = "")
    public static class QuestionBuilder {
    }
}
