package duoc.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDto {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("email")
    private String email;

    // Constructor sin argumentos
    public UserDto() {
    }

    // Constructor con todos los argumentos
    public UserDto(Long userId, String name, String documentId, String email) {
        this.userId = userId;
        this.name = name;
        this.documentId = documentId;
        this.email = email;
    }

    // Getters y Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", documentId='" + documentId + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
