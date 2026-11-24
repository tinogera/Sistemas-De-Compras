package com.Sistem.Solicitude_Compra.integraciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class ClienteN8nTest {

    private static final String URL = "http://n8n.test/webhook";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private ClienteN8n cliente(String secreto) {
        return new ClienteN8n(builder, JsonMapper.builder().build(), URL, secreto);
    }

    @Test
    void mandaElJsonPorPostAlWebhookSinFirmaSiNoHaySecreto() {
        ClienteN8n cliente = cliente("");
        server.expect(requestTo(URL + "/solicitud-creada"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist(ClienteN8n.HEADER_FIRMA))
                .andExpect(content().json("{\"solicitudId\":7}"))
                .andRespond(withSuccess());

        cliente.enviar("solicitud-creada", Map.of("solicitudId", 7));

        server.verify();
    }

    @Test
    void conSecretoFirmaElCuerpoConHmacSha256() {
        ClienteN8n cliente = cliente("secreto");
        String cuerpo = "{\"solicitudId\":7}";
        server.expect(requestTo(URL + "/solicitud-lista"))
                .andExpect(header(ClienteN8n.HEADER_FIRMA, ClienteN8n.firmar(cuerpo, "secreto")))
                .andRespond(withSuccess());

        cliente.enviar("solicitud-lista", Map.of("solicitudId", 7));

        server.verify();
    }

    @Test
    void laFirmaEsElHmacSha256EnHexadecimal() {
        // Valor de referencia: hmac.new(b"clave", b"hola", sha256).hexdigest() en Python.
        assertEquals("67c0066d5104af2bd88decc45269ddb529a0097b8da4cfbbe58ec2a5f63ca50d",
                ClienteN8n.firmar("hola", "clave"));
    }

    @Test
    void siN8nContestaConErrorLanzaParaQueElEventoQuedePendiente() {
        ClienteN8n cliente = cliente("");
        server.expect(requestTo(URL + "/solicitud-creada")).andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> cliente.enviar("solicitud-creada", Map.of()));
    }
}
