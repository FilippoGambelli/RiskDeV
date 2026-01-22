package it.unipi.riskDeV.exception;

public class NvdApiException extends RuntimeException {
    public NvdApiException(String message) {
        super(message);
    }

    public NvdApiException(String message, Throwable cause) {
        super(message, cause);
    }
}