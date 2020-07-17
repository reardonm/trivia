package trivia.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonDeserialize(builder = RoundCompleted.RoundCompletedBuilder.class)
@Value
@Builder
public class RoundCompleted implements GameMessage {

    private int round;

    private String answer;

    private List<Integer> stats;

    @JsonPOJOBuilder(withPrefix = "")
    public static class RoundCompletedBuilder {
    }
}
