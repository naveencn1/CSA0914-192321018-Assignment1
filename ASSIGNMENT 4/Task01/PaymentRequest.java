/** Input collected by the window and handed to the service. */
public record PaymentRequest(String studentId, String amountText, String mode) { }
