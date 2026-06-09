package service.room.dto;

import java.util.UUID;

import service.room.model.Room;

public record RoomAccessResponse(
        UUID roomId,
        UUID conferenceId,
        String name,
        String type,
        String locationOrLink,
        String topicHints,
        Integer capacity) {

    public static RoomAccessResponse from(Room room) {
        return new RoomAccessResponse(
                room.getId(),
                room.getConferenceId(),
                room.getName(),
                room.getType(),
                room.getLocationOrLink(),
                room.getTopicHints(),
                room.getCapacity());
    }
}
