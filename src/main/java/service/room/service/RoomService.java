package service.room.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import service.room.client.ConferenceClient;
import service.room.dto.RoomCreateRequest;
import service.room.dto.RoomResponse;
import service.room.exception.ConflictException;
import service.room.exception.NotFoundException;
import service.room.model.Room;
import service.room.repository.RoomRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ConferenceClient conferenceClient;

    public RoomService(RoomRepository roomRepository, ConferenceClient conferenceClient) {
        this.roomRepository = roomRepository;
        this.conferenceClient = conferenceClient;
    }

    @Transactional
    public RoomResponse create(UUID conferenceId, RoomCreateRequest request, String authHeader) {
        conferenceClient.ensureConferenceExists(conferenceId, authHeader);
        String normalizedName = normalize(request.name());
        if (roomRepository.existsByConferenceIdAndNameIgnoreCase(conferenceId, normalizedName)) {
            throw new ConflictException("Ya existe una sala con ese nombre en la conferencia");
        }

        Room room = new Room();
        room.setConferenceId(conferenceId);
        room.setName(normalizedName);
        room.setCapacity(request.capacity());
        room.setType(normalize(request.type()));
        room.setLocationOrLink(normalize(request.locationOrLink()));
        room.setTopicHints(optionalNormalize(request.topicHints()));
        Room saved = roomRepository.save(room);
        return RoomResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listByConference(UUID conferenceId) {
        return roomRepository.findByConferenceIdOrderByNameAsc(conferenceId)
                .stream()
                .map(RoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        return RoomResponse.from(room);
    }

    @Transactional
    public void delete(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        roomRepository.deleteById(roomId);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hay campos obligatorios vacios");
        }
        return value.trim();
    }

    private String optionalNormalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}