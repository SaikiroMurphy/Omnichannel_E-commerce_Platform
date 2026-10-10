package dan.edu.payment_service.common;

public class InvalidPaymentStateException extends RuntimeException {

	public InvalidPaymentStateException(String message) {
		super(message);
	}
}
