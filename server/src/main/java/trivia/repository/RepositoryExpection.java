package trivia.repository;

public class RepositoryExpection extends RuntimeException {
    public RepositoryExpection(String msg, Throwable e) {
        super(msg, e);
    }
}
