package duoc.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BffApplication {

	public static void main(String[] args) {
		SpringApplication.run(BffApplication.class, args);
	}

	/**
	 * Configura e inyecta RestTemplate como un bean singleton.
	 * Utilizado por FaasIntegrationService para realizar llamadas HTTP
	 * hacia los servicios Azure Functions.
	 * 
	 * La configuración de Jackson se aplica a través de application.properties:
	 * - spring.jackson.default-property-inclusion=non_null
	 * Esta configuración hace que se omitan campos null en todas las serializaciones.
	 *
	 * @return instancia de RestTemplate
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
