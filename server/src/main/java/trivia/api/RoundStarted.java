package trivia.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonDeserialize(builder = RoundStarted.RoundStartedBuilder.class)
@Value
@Builder
public class RoundStarted implements GameMessage {

    private int round;

    private String question;

    private List<String> answers;

    @JsonPOJOBuilder(withPrefix = "")
    public static class RoundStartedBuilder {
    }
}
