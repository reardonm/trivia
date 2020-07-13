package trivia.service;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import trivia.TestData;
import trivia.repository.QuestionRepository;

import javax.inject.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest
public class DefaultQuestionServiceSpec {

    @Inject
    QuestionRepository repository;

    @Inject
    ObjectMapper mapper;

    TestData data;

    DefaultQuestionService service;

    @MockBean(QuestionRepository.class)
    QuestionRepository questionRepository() {
        return mock(QuestionRepository.class);
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
}
