package duoc.bff.controller;

import duoc.bff.dto.LoanDto;
import duoc.bff.service.FaasIntegrationService;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestionar operaciones de préstamos de libros.
 * Orquesta las solicitudes hacia el servicio FaaS de préstamos.
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);

    private final FaasIntegrationService faasIntegrationService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param faasIntegrationService servicio de integración con FaaS
     */
    public LoanController(FaasIntegrationService faasIntegrationService) {
        this.faasIntegrationService = faasIntegrationService;
    }

    @GetMapping
    public ResponseEntity<Object> getAllLoans() {
        logger.info("Solicitando lista de préstamos vía GraphQL Query");
        
        String url = faasIntegrationService.getGraphqlQueryUrl();
        
        // Armamos el string literal de la Query de GraphQL
        String query = "{ loans { userId bookTitle } }";
        
        // Empaquetamos en un Map para que tu servicio lo convierta a JSON
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", query);
        
        // ¡OJO AQUÍ! GraphQL siempre viaja por POST, incluso para obtener datos (Queries)
        ResponseEntity<Object> response = faasIntegrationService.post(url, requestBody, Object.class);
        
        logger.info("Respuesta recibida del servicio FaaS GraphQL: {}", response.getStatusCode());
        return response;
    }

    @PostMapping
    public ResponseEntity<Object> createLoan(@RequestBody LoanDto loanDto) {
        logger.info("Creando nuevo préstamo vía GraphQL Mutation: {}", loanDto);
        
        String url = faasIntegrationService.getGraphqlMutationUrl();
        
        // Armamos el string literal de la Mutación inyectando los datos del DTO
        String mutation = String.format(
            "mutation { createLoan(userId: \"%s\", bookTitle: \"%s\") { success loan { userId bookTitle } } }",
            loanDto.getUserId(), loanDto.getBookTitle()
        );
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", mutation);
        
        ResponseEntity<Object> response = faasIntegrationService.post(url, requestBody, Object.class);
        
        logger.info("Préstamo creado exitosamente vía GraphQL. Status: {}", response.getStatusCode());
        return response;
    }
}
