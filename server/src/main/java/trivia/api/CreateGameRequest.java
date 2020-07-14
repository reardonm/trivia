package trivia.api;

import io.micronaut.core.annotation.Introspected;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Introspected
@Data
public class CreateGameRequest {

    @NotBlank
    private String category;
}
