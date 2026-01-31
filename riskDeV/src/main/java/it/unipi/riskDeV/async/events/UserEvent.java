package it.unipi.riskDeV.async.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserEvent.UserUpdated.class, name = "UserUpdated")
})
public sealed interface UserEvent {
    String username(); 

    record UserUpdated(List<String> projectNames, String username, String newUsername, String newEmail) implements UserEvent {}
}