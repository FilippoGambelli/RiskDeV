package it.unipi.riskDeV.results; 

public sealed interface Result<T> permits Result.Success, Result.Failure {
    record Success<T>(T data) implements Result<T> {}
    record Failure<T>(DomainError error) implements Result<T> {}

    static <T> Result<T> success(T data) {
        return new Success<>(data);
    }

    static <T> Result<T> failure(DomainError error) {
        return new Failure<>(error);
    }
}