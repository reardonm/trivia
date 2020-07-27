package trivia.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import trivia.domain.Question;
import trivia.domain.RoundEvent;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class JsonEncoder {

    private final ObjectMapper mapper;

    public JsonEncoder(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    Question decodeQuestion(String data) {
        try {
            return mapper.readValue(data, Question.class);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to decode Question from repository", e);
        }
    }

     String encodeQuestion(Question q) {
        try {
            return mapper.writeValueAsString(q);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to encode Question to repository", e);
        }
    }

     RoundEvent decodeRoundEvent(String data) {
        try {
            return mapper.readValue(data, RoundEvent.class);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to decode RoundEvent from repository", e);
        }
    }

     String encodeRoundEvent(RoundEvent r) {
        try {
            return mapper.writeValueAsString(r);
        } catch (JsonProcessingException e) {
            throw new RepositoryExpection("Failed to encode RoundEvent to repository", e);
        }
    }
}
