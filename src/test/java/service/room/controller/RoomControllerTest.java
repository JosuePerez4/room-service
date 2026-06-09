package service.room.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import service.room.dto.RoomAccessResponse;
import service.room.exception.ForbiddenException;
import service.room.exception.NotFoundException;
import service.room.service.RoomService;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    @Test
    void enterRoom_whenSuccess_returnsOkAndDetails() {
        UUID roomId = UUID.randomUUID();
        UUID conferenceId = UUID.randomUUID();
        RoomAccessResponse accessResponse = new RoomAccessResponse(
                roomId, conferenceId, "Sala A", "VIRTUAL", "http://zoom.us", "AI", 100
        );

        when(roomService.enterRoom(roomId, "Bearer valid_token")).thenReturn(accessResponse);

        RoomAccessResponse response = roomController.enter(roomId, "Bearer valid_token");

        assertNotNull(response);
        assertEquals(roomId, response.roomId());
        assertEquals(conferenceId, response.conferenceId());
        assertEquals("Sala A", response.name());
        assertEquals("VIRTUAL", response.type());
        assertEquals("http://zoom.us", response.locationOrLink());

        verify(roomService).enterRoom(roomId, "Bearer valid_token");
    }

    @Test
    void enterRoom_whenForbidden_throwsForbidden() {
        UUID roomId = UUID.randomUUID();

        when(roomService.enterRoom(roomId, "Bearer invalid_token"))
                .thenThrow(new ForbiddenException("No autorizado"));

        assertThrows(ForbiddenException.class, () -> {
            roomController.enter(roomId, "Bearer invalid_token");
        });
    }

    @Test
    void enterRoom_whenNotFound_throwsNotFound() {
        UUID roomId = UUID.randomUUID();

        when(roomService.enterRoom(roomId, "Bearer token"))
                .thenThrow(new NotFoundException("Sala no encontrada"));

        assertThrows(NotFoundException.class, () -> {
            roomController.enter(roomId, "Bearer token");
        });
    }
}
