package trivia.api;

import lombok.Data;

import java.util.List;

@Data
public class CategoriesResponse {
    private List<String> categories;

}
