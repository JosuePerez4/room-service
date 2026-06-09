package service.room.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import service.room.client.ConferenceClient;
import service.room.client.RegistrationClient;
import service.room.dto.RoomAccessResponse;
import service.room.exception.ForbiddenException;
import service.room.exception.NotFoundException;
import service.room.model.Room;
import service.room.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ConferenceClient conferenceClient;

    @Mock
    private RegistrationClient registrationClient;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, conferenceClient, registrationClient);
    }

    @Test
    void enterRoom_whenRoomNotFound_throwsNotFoundException() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            roomService.enterRoom(roomId, "Bearer token");
        });

        verify(registrationClient, never()).ensureUserHasActiveRegistration(any(), any());
    }

    @Test
    void enterRoom_whenRoomExistsButRegistrationFails_throwsForbiddenException() {
        UUID roomId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Room room = new Room();
        room.setId(roomId);
        room.setConferenceId(conferenceId);
        room.setName("Sala A");
        room.setType("VIRTUAL");
        room.setLocationOrLink("http://link");

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        doThrow(new ForbiddenException("No autorizado"))
                .when(registrationClient).ensureUserHasActiveRegistration(conferenceId, "Bearer token");

        assertThrows(ForbiddenException.class, () -> {
            roomService.enterRoom(roomId, "Bearer token");
        });
    }

    @Test
    void enterRoom_whenRoomExistsAndRegistrationActive_returnsAccessDetails() {
        UUID roomId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        Room room = new Room();
        room.setId(roomId);
        room.setConferenceId(conferenceId);
        room.setName("Sala A");
        room.setType("VIRTUAL");
        room.setLocationOrLink("http://link");
        room.setCapacity(100);
        room.setTopicHints("Tech");

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        doNothing().when(registrationClient).ensureUserHasActiveRegistration(conferenceId, "Bearer token");

        RoomAccessResponse response = roomService.enterRoom(roomId, "Bearer token");

        assertNotNull(response);
        assertEquals(roomId, response.roomId());
        assertEquals(conferenceId, response.conferenceId());
        assertEquals("Sala A", response.name());
        assertEquals("VIRTUAL", response.type());
        assertEquals("http://link", response.locationOrLink());
        assertEquals("Tech", response.topicHints());
        assertEquals(100, response.capacity());

        verify(registrationClient, times(1)).ensureUserHasActiveRegistration(conferenceId, "Bearer token");
    }
}
