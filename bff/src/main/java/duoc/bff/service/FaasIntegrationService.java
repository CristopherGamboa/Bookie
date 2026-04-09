package duoc.bff.service;

import duoc.bff.config.DtoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Servicio de integración con Azure Functions (FaaS).
 * Proporciona métodos genéricos para orquestar llamadas HTTP hacia las funciones serverless.
 */
@Service
public class FaasIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(FaasIntegrationService.class);

    private final RestTemplate restTemplate;

    @Value("${faas.user.url}")
    private String userServiceUrl;

    @Value("${faas.graphql.query.url}")
    private String graphqlQueryUrl;

    @Value("${faas.graphql.mutation.url}")
    private String graphqlMutationUrl;

    /**
     * Constructor con inyección de dependencias para RestTemplate.
     *
     * @param restTemplate instancia de RestTemplate configurada en la aplicación
     */
    public FaasIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Realiza una llamada GET genérica a una URL de FaaS.
     *
     * @param url URL completa del endpoint FaaS
     * @param responseType clase del tipo de respuesta esperado
     * @return ResponseEntity con la respuesta del FaaS
     */
    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        logger.info("Realizando GET a: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            logger.info("Respuesta GET exitosa. Status: {}", response.getStatusCode());
            
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error HTTP en llamada GET a {}: {} - Respuesta: {}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Error HTTP " + e.getStatusCode() + " al consultar el servicio FaaS: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Error en llamada GET a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al consultar el servicio FaaS", e);
        }
    }

    /**
     * Realiza una llamada POST genérica a una URL de FaaS.
     * 
     * El body se convierte a Map para excluir campos null automáticamente,
     * luego se serializa a JSON String puro para garantizar compatibilidad
     * con Azure Functions que espera HttpRequestMessage<Optional<String>>.
     *
     * @param url URL completa del endpoint FaaS
     * @param requestBody objeto a enviar en el body de la solicitud
     * @param responseType clase del tipo de respuesta esperado
     * @return ResponseEntity con la respuesta del FaaS
     */
    public <T> ResponseEntity<T> post(String url, Object requestBody, Class<T> responseType) {
        logger.info("=== INICIANDO POST ===");
        logger.info("URL destino: {}", url);
        
        try {
            // Validar que el body no sea nulo
            if (requestBody == null) {
                logger.warn("El body de la solicitud es nulo");
                throw new IllegalArgumentException("El cuerpo de la solicitud no puede ser nulo");
            }
            
            logger.info("Clase del body: {}", requestBody.getClass().getSimpleName());
            logger.info("Body original (toString): {}", requestBody);
            
            // Convertir a Map excluyendo campos null
            Object bodyToSend = DtoConverter.toMapExcludingNull(requestBody);
            logger.info("Body convertido a Map (sin nulls): {}", bodyToSend);
            
            // Serializar Map a JSON String puro para Azure Functions
            // Azure Functions espera HttpRequestMessage<Optional<String>>, no un objeto serializado
            String jsonBody = mapToJsonString((Map) bodyToSend);
            logger.info("Body serializado a JSON String: {}", jsonBody);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.add("User-Agent", "BFF-Spring-Boot/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            logger.info("Headers configurados:");
            headers.forEach((name, values) -> logger.info("  {}: {}", name, values));
            
            logger.info("Enviando solicitud POST con body tipo String...");
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            
            logger.info("=== POST EXITOSO ===");
            logger.info("Status Code: {}", response.getStatusCode());
            logger.info("Response Headers: {}", response.getHeaders());
            if (response.getBody() != null) {
                logger.info("Response Body: {}", response.getBody());
            }
            
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("=== ERROR HTTP ===");
            logger.error("Status: {}", e.getStatusCode());
            logger.error("Respuesta del servidor: {}", e.getResponseBodyAsString());
            logger.error("Causas posibles:");
            logger.error("  1. Formato JSON incorrecto");
            logger.error("  2. Campos esperados no presentes");
            logger.error("  3. Headers HTTP incorrectos");
            logger.error("  4. Azure Function no accesible o desconfigurada");
            
            throw new RuntimeException("Error HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("=== ERROR INESPERADO ===");
            logger.error("Mensaje: {}", e.getMessage());
            logger.error("Causa: {}", e.getCause());
            logger.error("Tipo: {}", e.getClass().getName());
            throw new RuntimeException("Error al invocar el servicio FaaS", e);
        }
    }
    
    /**
     * Convierte un Map a formato JSON String.
     * Escapa caracteres especiales en valores String para JSON válido.
     * 
     * @param map mapa con clave-valor a convertir
     * @return JSON String con formato {"clave1": "valor1", "clave2": valor2}
     */
    private String mapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        
        map.forEach((key, value) -> {
            if (sb.length() > 1) {
                sb.append(",");
            }
            
            sb.append("\"").append(escapeJsonString(key)).append("\":");
            
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJsonString((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                // Para otros tipos, usa toString
                sb.append("\"").append(escapeJsonString(value.toString())).append("\"");
            }
        });
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Escapa caracteres especiales en strings para formato JSON válido.
     * 
     * @param str string a escapar
     * @return string escapado para JSON
     */
    private String escapeJsonString(String str) {
        if (str == null) return "";
        
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Obtiene la URL del servicio de usuarios.
     *
     * @return URL configurada para el servicio de usuarios
     */
    public String getUserServiceUrl() {
        return userServiceUrl;
    }

    public String getGraphqlQueryUrl() {
        return graphqlQueryUrl;
    }

    public String getGraphqlMutationUrl() {
        return graphqlMutationUrl;
    }
}
