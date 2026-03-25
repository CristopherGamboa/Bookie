package duoc.bff.controller;

import duoc.bff.dto.LoanDto;
import duoc.bff.service.FaasIntegrationService;
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

    /**
     * Obtiene la lista de todos los préstamos.
     * Realiza una llamada GET al servicio FaaS de préstamos.
     *
     * @return ResponseEntity con la lista de préstamos
     */
    @GetMapping
    public ResponseEntity<Object> getAllLoans() {
        logger.info("Solicitando lista de préstamos");
        
        String url = faasIntegrationService.getLoanServiceUrl();
        ResponseEntity<Object> response = faasIntegrationService.get(url, Object.class);
        
        logger.info("Respuesta recibida del servicio FaaS: {}", response.getStatusCode());
        return response;
    }

    /**
     * Crea un nuevo préstamo de libro.
     * Realiza una llamada POST al servicio FaaS de préstamos con los datos del préstamo.
     *
     * @param loanDto datos del préstamo a crear
     * @return ResponseEntity con la respuesta del FaaS (préstamo creado)
     */
    @PostMapping
    public ResponseEntity<Object> createLoan(@RequestBody LoanDto loanDto) {
        logger.info("Creando nuevo préstamo: {}", loanDto);
        logger.debug("LoanDto recibido: loanId={}, userId={}, bookTitle={}, loanDate={}, returnDate={}, status={}", 
                loanDto.getLoanId(), loanDto.getUserId(), loanDto.getBookTitle(), 
                loanDto.getLoanDate(), loanDto.getReturnDate(), loanDto.getStatus());
        
        String url = faasIntegrationService.getLoanServiceUrl();
        ResponseEntity<Object> response = faasIntegrationService.post(url, loanDto, Object.class);
        
        logger.info("Préstamo creado exitosamente. Status: {}", response.getStatusCode());
        return response;
    }
}
