package trivia.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = RoundEvent.RoundEventBuilder.class)
@Value
@Builder
public class RoundEvent {

    private String gameId;

    private Integer round;

    private Boolean started;

    @JsonPOJOBuilder(withPrefix = "")
    public static class RoundEventBuilder {
    }
}
