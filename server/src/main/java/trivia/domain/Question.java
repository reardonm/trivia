package trivia.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonDeserialize(builder = Question.QuestionBuilder.class)
@Value
@Builder
public class Question {

    private String id;

    private String category;

    private Difficulty difficulty;

    private String question;

    private String correctAnswer;

    private List<String> incorrectAnswers;


    @JsonPOJOBuilder(withPrefix = "")
    public static class QuestionBuilder {
    }
}
