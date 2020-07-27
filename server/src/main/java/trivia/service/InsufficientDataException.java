package trivia.service;

public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String s) {
        super(s);
    }
}
