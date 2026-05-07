package service.room.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank(message = "El nombre de la sala es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String name,
        @Min(value = 1, message = "La capacidad debe ser mayor a 0")
        Integer capacity,
        @NotBlank(message = "El tipo de sala es obligatorio")
        @Size(max = 80, message = "El tipo no puede superar 80 caracteres")
        String type,
        @NotBlank(message = "La ubicación o link es obligatorio")
        @Size(max = 255, message = "La ubicación o link no puede superar 255 caracteres")
        String locationOrLink,
        @Size(max = 255, message = "topicHints no puede superar 255 caracteres")
        String topicHints) {
}
