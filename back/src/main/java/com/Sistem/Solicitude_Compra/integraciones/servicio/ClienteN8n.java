package com.Sistem.Solicitude_Compra.integraciones.servicio;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Llama a los webhooks de n8n. Si n8n no responde o contesta con error lanza una excepción: el listener
 * falla y el evento queda pendiente en {@code event_publication} para reintentarse (§9).
 */
@Component
public class ClienteN8n {

    static final String HEADER_FIRMA = "X-Firma";

    private final RestClient cliente;
    private final ObjectMapper mapper;
    private final String secreto;

    @Autowired
    public ClienteN8n(ObjectMapper mapper, @Value("${app.n8n.webhook-url}") String urlBase,
                      @Value("${app.n8n.webhook-secret:}") String secreto) {
        this(RestClient.builder().requestFactory(conHttp11()), mapper, urlBase, secreto);
    }

    // El cliente del JDK intenta pasar a HTTP/2 en texto plano y n8n no contesta: el pedido se cuelga.
    // Por eso se fuerza HTTP/1.1; y los timeouts hacen que un n8n caído falle (y el evento se reintente).
    private static JdkClientHttpRequestFactory conHttp11() {
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }

    // Para los tests, que le pasan un builder atado a un servidor simulado.
    ClienteN8n(RestClient.Builder builder, ObjectMapper mapper, String urlBase, String secreto) {
        this.cliente = builder.baseUrl(urlBase).build();
        this.mapper = mapper;
        this.secreto = secreto;
    }

    public void enviar(String webhook, Object datos) {
        String cuerpo = mapper.writeValueAsString(datos);
        RestClient.RequestBodySpec pedido = cliente.post().uri("/" + webhook).contentType(MediaType.APPLICATION_JSON);
        if (!secreto.isBlank()) {
            pedido.header(HEADER_FIRMA, firmar(cuerpo, secreto));
        }
        pedido.body(cuerpo).retrieve().toBodilessEntity();
    }

    /** HMAC-SHA256 en hexadecimal de los bytes exactos que se mandan en el body. */
    static String firmar(String cuerpo, String secreto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar el webhook", e);
        }
    }
}
