package service.room.dto;

import java.time.Instant;
import java.util.UUID;

import service.room.model.Room;

public record RoomResponse(
        UUID id,
        UUID conferenceId,
        String name,
        Integer capacity,
        String type,
        String locationOrLink,
        String topicHints,
        Instant createdAt) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getConferenceId(),
                room.getName(),
                room.getCapacity(),
                room.getType(),
                room.getLocationOrLink(),
                room.getTopicHints(),
                room.getCreatedAt());
    }
}
