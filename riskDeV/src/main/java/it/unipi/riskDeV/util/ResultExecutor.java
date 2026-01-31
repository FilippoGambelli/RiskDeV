package it.unipi.riskDeV.util;
import java.util.concurrent.Callable;

import it.unipi.riskDeV.results.DomainError;
import it.unipi.riskDeV.results.Result;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultExecutor {
    public static <T> Result<T> execute(Callable<Result<T>> action) {
        try {
            return action.call();
            } 
        catch (Exception e) {
            log.error("Unhandled exception caught in executor", e);
            return new Result.Failure<>(new DomainError.SystemError(e));
        }
    }
}