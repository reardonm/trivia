package trivia.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = PlayerJoined.PlayerJoinedBuilder.class)
@Value
@Builder
public class PlayerJoined implements GameMessage {
    private String username;
    private int playerCount;

    @JsonPOJOBuilder(withPrefix = "")
    public static class PlayerJoinedBuilder {
    }
}
