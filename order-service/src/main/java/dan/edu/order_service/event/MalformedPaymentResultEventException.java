package dan.edu.order_service.event;

public class MalformedPaymentResultEventException extends RuntimeException {

	public MalformedPaymentResultEventException(String message, Throwable cause) {
		super(message, cause);
	}
}
