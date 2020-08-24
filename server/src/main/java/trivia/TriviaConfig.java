package trivia;


import io.micronaut.context.annotation.ConfigurationProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@ConfigurationProperties("trivia")
public class TriviaConfig {

    @NotBlank
    private String dataPath;

    @Positive
    private int roundsPerGame;

    @Positive
    private int minimumPlayersPerGame;


    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public int getRoundsPerGame() {
        return roundsPerGame;
    }

    public void setRoundsPerGame(int roundsPerGame) {
        this.roundsPerGame = roundsPerGame;
    }

    public int getMinimumPlayersPerGame() {
        return minimumPlayersPerGame;
    }

    public void setMinimumPlayersPerGame(int minimumPlayersPerGame) {
        this.minimumPlayersPerGame = minimumPlayersPerGame;
    }

}
