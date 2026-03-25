package duoc.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoanDto {

    @JsonProperty("loanId")
    private Long loanId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("bookTitle")
    private String bookTitle;

    @JsonProperty("loanDate")
    private String loanDate;

    @JsonProperty("returnDate")
    private String returnDate;

    @JsonProperty("status")
    private String status;

    // Constructor sin argumentos
    public LoanDto() {
    }

    // Constructor con todos los argumentos
    public LoanDto(Long loanId, Long userId, String bookTitle, String loanDate, String returnDate, String status) {
        this.loanId = loanId;
        this.userId = userId;
        this.bookTitle = bookTitle;
        this.loanDate = loanDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters y Setters
    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(String loanDate) {
        this.loanDate = loanDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LoanDto{" +
                "loanId=" + loanId +
                ", userId=" + userId +
                ", bookTitle='" + bookTitle + '\'' +
                ", loanDate='" + loanDate + '\'' +
                ", returnDate='" + returnDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
