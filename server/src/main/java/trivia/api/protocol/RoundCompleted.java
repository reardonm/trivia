package trivia.api.protocol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@JsonDeserialize(builder = RoundCompleted.RoundCompletedBuilder.class)
@Value
@Builder
public class RoundCompleted implements GameMessage {

    private int round;

    private String answer;

    private Map<String,Integer> stats;

    @JsonPOJOBuilder(withPrefix = "")
    public static class RoundCompletedBuilder {
    }
}
