package service.room.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import service.room.model.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByConferenceIdAndNameIgnoreCase(UUID conferenceId, String name);

    List<Room> findByConferenceIdOrderByNameAsc(UUID conferenceId);

    Optional<Room> findByIdAndConferenceId(UUID id, UUID conferenceId);
}
