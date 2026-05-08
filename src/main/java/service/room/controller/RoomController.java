package service.room.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import service.room.dto.RoomCreateRequest;
import service.room.dto.RoomResponse;
import service.room.service.RoomService;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/conference/{conferenceId}")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(
            @PathVariable UUID conferenceId,
            @Valid @RequestBody RoomCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return roomService.create(conferenceId, request, authHeader);
    }

    @GetMapping("/conference/{conferenceId}")
    public List<RoomResponse> listByConference(@PathVariable UUID conferenceId) {
        return roomService.listByConference(conferenceId);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getById(@PathVariable UUID roomId) {
        return roomService.getById(roomId);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID roomId) {
        roomService.delete(roomId);
    }
}