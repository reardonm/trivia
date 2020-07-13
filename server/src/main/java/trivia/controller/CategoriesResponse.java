package trivia.controller;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class CategoriesResponse {
    private List<String> categories;
}
