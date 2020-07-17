package trivia.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value=PlayerJoined.class, name = "player_joined"),
    @JsonSubTypes.Type(value=GameStarted.class, name = "game_started"),
    @JsonSubTypes.Type(value=RoundStarted.class, name = "round_started"),
    @JsonSubTypes.Type(value=RoundCompleted.class, name = "round_completed"),
    @JsonSubTypes.Type(value=PlayerEliminated.class, name = "player_eliminated"),
    @JsonSubTypes.Type(value=PlayerAdvanced.class, name = "player_advanced")

})
public interface GameMessage {

}
