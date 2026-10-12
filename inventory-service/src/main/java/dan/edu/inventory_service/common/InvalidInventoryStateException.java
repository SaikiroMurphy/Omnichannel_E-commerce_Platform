package dan.edu.inventory_service.common;

public class InvalidInventoryStateException extends RuntimeException {

	public InvalidInventoryStateException(String message) {
		super(message);
	}
}
