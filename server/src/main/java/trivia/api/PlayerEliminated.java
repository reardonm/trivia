package trivia.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = PlayerEliminated.PlayerEliminatedBuilder.class)
@Value
@Builder
public class PlayerEliminated implements GameMessage {

    private String username;

    @JsonPOJOBuilder(withPrefix = "")
    public static class PlayerEliminatedBuilder {
    }
}
