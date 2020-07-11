package trivia.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class TrivaControllerSpec {

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/api")
    HttpClient client;

    @BeforeEach
    void setUp() {
    }

    @Test
    void landingPage() {
        String response = client.toBlocking().retrieve(HttpRequest.GET("/"));
        assertThat(response).isNotBlank();
    }

}
