package it.unipi.riskDeV.async.events;

public class UserEvents {
    public record UserCreatedEvent(String username) {}

    public record UserDeletedEvent(String username) {}

    public record UserUpdatedEvent(String oldUsername, String newUsername) {}
}
