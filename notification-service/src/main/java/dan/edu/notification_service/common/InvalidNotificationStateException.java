package dan.edu.notification_service.common;

public class InvalidNotificationStateException extends RuntimeException {

	public InvalidNotificationStateException(String message) {
		super(message);
	}
}
