package it.unipi.riskDeV.exception;

public class PackageNotFoundExeption extends RuntimeException {
    public PackageNotFoundExeption(String message) {
        super(message);
    }
}
