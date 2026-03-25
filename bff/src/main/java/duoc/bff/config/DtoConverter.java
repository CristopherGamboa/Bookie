package duoc.bff.config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para convertir DTOs a Maps excludyendo campos null.
 * Esto asegura que RestTemplate no envíe campos con valor null.
 */
public class DtoConverter {

    /**
     * Convierte un objeto DTO a un Map, excluyendo campos null.
     * 
     * @param dto objeto DTO a convertir
     * @return Map con los campos del DTO (sin nulos)
     */
    public static Map<String, Object> toMapExcludingNull(Object dto) {
        Map<String, Object> map = new HashMap<>();
        
        if (dto == null) {
            return map;
        }
        
        try {
            // Obtener todos los campos del DTO (incluyendo heredados)
            Field[] fields = getAllFields(dto.getClass());
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(dto);
                
                // Solo añadir si el valor no es null
                if (value != null) {
                    map.put(field.getName(), value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error al convertir DTO a Map", e);
        }
        
        return map;
    }

    /**
     * Obtiene todos los campos de una clase, incluyendo heredados.
     */
    private static Field[] getAllFields(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        
        return fields.values().toArray(new Field[0]);
    }
}
