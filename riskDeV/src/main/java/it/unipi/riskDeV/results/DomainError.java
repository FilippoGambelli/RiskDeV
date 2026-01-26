package it.unipi.riskDeV.results; 

public sealed interface DomainError permits 
    DomainError.NotFound, 
    DomainError.AlreadyExists,
    DomainError.SystemError,
    DomainError.InvalidCredentials,
    DomainError.ValidationFailed,
    DomainError.AccessDenied,
    DomainError.InvalidOperation {

    String message();

    record NotFound(String message) implements DomainError {}
    record AlreadyExists(String message) implements DomainError {}
    record SystemError(String message, Throwable cause) implements DomainError {}
    record InvalidCredentials(String message) implements DomainError {}
    record ValidationFailed(String message) implements DomainError {}
    record AccessDenied(String message) implements DomainError {}
    record InvalidOperation(String message) implements DomainError {}
}
