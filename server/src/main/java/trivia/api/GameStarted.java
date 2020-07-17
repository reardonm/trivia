package trivia.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = GameStarted.GameStartedBuilder.class)
@Value
@Builder
public class GameStarted implements GameMessage {

    private String id;

    @JsonPOJOBuilder(withPrefix = "")
    public static class GameStartedBuilder {
    }
}
