package dan.edu.billing_service.common;

public class InvalidInvoiceStateException extends RuntimeException {

	public InvalidInvoiceStateException(String message) {
		super(message);
	}
}
