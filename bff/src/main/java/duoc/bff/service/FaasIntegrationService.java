package duoc.bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Value("${faas.loan.url}")
    private String loanServiceUrl;

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
            return restTemplate.getForEntity(url, responseType);
        } catch (Exception e) {
            logger.error("Error en llamada GET a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al consultar el servicio FaaS", e);
        }
    }

    /**
     * Realiza una llamada POST genérica a una URL de FaaS.
     *
     * @param url URL completa del endpoint FaaS
     * @param requestBody objeto a enviar en el body de la solicitud
     * @param responseType clase del tipo de respuesta esperado
     * @return ResponseEntity con la respuesta del FaaS
     */
    public <T> ResponseEntity<T> post(String url, Object requestBody, Class<T> responseType) {
        logger.info("Realizando POST a: {}", url);
        try {
            HttpEntity<Object> entity = new HttpEntity<>(requestBody);
            return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
        } catch (Exception e) {
            logger.error("Error en llamada POST a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al invocar el servicio FaaS", e);
        }
    }

    /**
     * Obtiene la URL del servicio de usuarios.
     *
     * @return URL configurada para el servicio de usuarios
     */
    public String getUserServiceUrl() {
        return userServiceUrl;
    }

    /**
     * Obtiene la URL del servicio de préstamos.
     *
     * @return URL configurada para el servicio de préstamos
     */
    public String getLoanServiceUrl() {
        return loanServiceUrl;
    }
}
