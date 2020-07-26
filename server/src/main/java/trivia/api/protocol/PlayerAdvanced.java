package trivia.api.protocol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = PlayerAdvanced.PlayerAdvancedBuilder.class)
@Value
@Builder
public class PlayerAdvanced implements GameMessage {

    private String username;

    @JsonPOJOBuilder(withPrefix = "")
    public static class PlayerAdvancedBuilder {
    }
}
