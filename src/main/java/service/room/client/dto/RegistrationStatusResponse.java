package service.room.client.dto;

import java.util.UUID;

public record RegistrationStatusResponse(
    boolean paid,
    UUID registrationId,
    String paymentStatus
) {}
