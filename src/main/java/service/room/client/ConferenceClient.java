package service.room.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import service.room.exception.NotFoundException;

@Component
public class ConferenceClient {

    private final RestClient restClient;

    public ConferenceClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.conference.base-url}") String conferenceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(conferenceBaseUrl).build();
    }

    public void ensureConferenceExists(UUID conferenceId, String authorizationHeader) {
        try {
            restClient.get()
                    .uri("/conferences/get/{id}", conferenceId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException("La conferencia no existe");
            }
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo validar la conferencia: " + ex.getMessage());
        }
    }
}