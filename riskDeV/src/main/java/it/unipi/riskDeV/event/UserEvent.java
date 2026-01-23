package it.unipi.riskDeV.event;

public class UserEvent {
    public record UserCreatedEvent(String username) {}

    public record UserDeletedEvent(String username) {}

    public record UserUpdatedEvent(String oldUsername, String newUsername) {}
}
