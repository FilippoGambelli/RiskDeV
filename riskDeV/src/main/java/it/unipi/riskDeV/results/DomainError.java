package it.unipi.riskDeV.results; 

public sealed interface DomainError permits 
    DomainError.NotFound, 
    DomainError.AlreadyExists,
    DomainError.SystemError,
    DomainError.InvalidCredentials,
    DomainError.ValidationFailed,
    DomainError.AccessDenied,
    DomainError.InvalidOperation {

    default String message() {
        return "API request failed! Please try again later.";
    }

    record NotFound(String message) implements DomainError {}
    record AlreadyExists(String message) implements DomainError {}
    record SystemError() implements DomainError {}
    record InvalidCredentials(String message) implements DomainError {}
    record ValidationFailed(String message) implements DomainError {}
    record AccessDenied(String message) implements DomainError {}
    record InvalidOperation(String message) implements DomainError {}
}
