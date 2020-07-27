package trivia.service;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import trivia.TestData;
import trivia.domain.Question;
import trivia.repository.GameRepository;

import javax.inject.Inject;
import javax.naming.InsufficientResourcesException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
public class DefaultQuestionServiceSpec {

    @Inject
    GameRepository repository;

    @Inject
    ObjectMapper mapper;

    TestData data;

    DefaultQuestionService service;

    @MockBean(GameRepository.class)
    GameRepository repository() {
        return mock(GameRepository.class);
    }

    @BeforeEach
    void setup() {
        service = new DefaultQuestionService(repository);
        data = TestData.load(mapper);
    }

    @Test
    void listCategories() {
        var cats = data.getCategories();
        when(repository.listCategories()).thenReturn(Flux.fromStream(cats.stream()));

        List<String> result = service.listCategories().collectList().block();
        assertThat(result).containsExactly(cats.toArray(new String[0]));
    }

    @Test
    void allocateQuestions() {
        String category = "Entertainment: Sports";
        List<Question> qs = data.getQuestions().subList(0, 5);
        when(repository.allocateQuestions(category, 5)).thenReturn(Flux.fromIterable(qs));

        List<Question> questions = service.allocateQuestions(category, 5).block();
        assertThat(questions).hasSize(5);
    }

    @Test
    void allocateQuestions_NotEnough() {
        String category = "Entertainment: Sports";
        List<Question> qs = data.getQuestions().subList(0, 1);
        when(repository.allocateQuestions(category, 5)).thenReturn(Flux.fromIterable(qs));

        Exception ex = assertThrows(InsufficientDataException.class, () -> {
            service.allocateQuestions(category, 5).block();
        });

        assertThat(ex.getMessage()).isEqualTo("Not enough questions to allocate 5");
    }
}
