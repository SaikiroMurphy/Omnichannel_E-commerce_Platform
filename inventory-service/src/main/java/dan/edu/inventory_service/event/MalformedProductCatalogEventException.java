package dan.edu.inventory_service.event;

public class MalformedProductCatalogEventException extends RuntimeException {

	public MalformedProductCatalogEventException(String message, Throwable cause) {
		super(message, cause);
	}
}
