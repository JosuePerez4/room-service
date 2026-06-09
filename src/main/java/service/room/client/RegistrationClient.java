package service.room.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import service.room.client.dto.RegistrationStatusResponse;
import service.room.exception.ForbiddenException;

@Component
public class RegistrationClient {

    private final RestClient restClient;

    public RegistrationClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.registration.base-url}") String registrationBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(registrationBaseUrl).build();
    }

    public void ensureUserHasActiveRegistration(UUID conferenceId, String authorizationHeader) {
        try {
            RegistrationStatusResponse response = restClient.get()
                    .uri("/registrations/payment-status?conferenceId={conferenceId}", conferenceId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(RegistrationStatusResponse.class);

            if (response == null || !response.paid()) {
                throw new ForbiddenException("El usuario no tiene una inscripción activa y aprobada en esta conferencia");
            }
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ForbiddenException("No autorizado para acceder a esta sala");
            }
            throw ex;
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo validar la inscripción: " + ex.getMessage());
        }
    }
}
