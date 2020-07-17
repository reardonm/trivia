package trivia.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = Question.QuestionBuilder.class)
@Value
@Builder
public class Game {
    private String id;
    private String category;
    private int players;
    private boolean started;
}
