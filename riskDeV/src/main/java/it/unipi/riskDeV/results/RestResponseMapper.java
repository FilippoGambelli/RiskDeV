package it.unipi.riskDeV.results;

import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import it.unipi.riskDeV.DTO.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RestResponseMapper {

    public <T> ResponseEntity<?> map(Result<T> result, HttpStatus successStatus) {
        return switch (result) {
            case Result.Success<T> s -> ResponseEntity.status(successStatus).body(s.data());
            case Result.Failure<T> f -> mapError(f.error());
        };
    }

    public ResponseEntity<ErrorResponseDTO> mapError(DomainError error) {
        ErrorResponseDTO payload = ErrorResponseDTO.withMessage(error.message());

        return switch (error) {
            case DomainError.NotFound e -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(payload);
            case DomainError.AlreadyExists e -> ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
            case DomainError.InvalidCredentials e -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(payload);
            case DomainError.ValidationFailed e -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
            case DomainError.AccessDenied e -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(payload);
            case DomainError.InvalidOperation e -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
            case DomainError.SystemError e -> {
                log.error("System Error caught: ", e.exception());
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
            }
        };
    }
}
