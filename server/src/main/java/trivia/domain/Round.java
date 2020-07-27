package trivia.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Round {
    private int number;

    private Question question;

    private int players;
}
