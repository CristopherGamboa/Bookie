package duoc.bff.controller;

import duoc.bff.dto.UserDto;
import duoc.bff.service.FaasIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestionar operaciones de usuarios.
 * Orquesta las solicitudes hacia el servicio FaaS de usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final FaasIntegrationService faasIntegrationService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param faasIntegrationService servicio de integración con FaaS
     */
    public UserController(FaasIntegrationService faasIntegrationService) {
        this.faasIntegrationService = faasIntegrationService;
    }

    /**
     * Obtiene la lista de todos los usuarios.
     * Realiza una llamada GET al servicio FaaS de usuarios.
     *
     * @return ResponseEntity con la lista de usuarios
     */
    @GetMapping
    public ResponseEntity<Object> getAllUsers() {
        logger.info("Solicitando lista de usuarios");
        
        String url = faasIntegrationService.getUserServiceUrl();
        ResponseEntity<Object> response = faasIntegrationService.get(url, Object.class);
        
        logger.info("Respuesta recibida del servicio FaaS: {}", response.getStatusCode());
        return response;
    }

    /**
     * Crea un nuevo usuario.
     * Realiza una llamada POST al servicio FaaS de usuarios con los datos del usuario.
     *
     * @param userDto datos del usuario a crear
     * @return ResponseEntity con la respuesta del FaaS (usuario creado)
     */
    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody UserDto userDto) {
        logger.info("Creando nuevo usuario: {}", userDto);
        logger.debug("UserDto recibido: userId={}, name={}, documentId={}, email={}", 
                userDto.getUserId(), userDto.getName(), userDto.getDocumentId(), userDto.getEmail());
        
        String url = faasIntegrationService.getUserServiceUrl();
        ResponseEntity<Object> response = faasIntegrationService.post(url, userDto, Object.class);
        
        logger.info("Usuario creado exitosamente. Status: {}", response.getStatusCode());
        return response;
    }
}
