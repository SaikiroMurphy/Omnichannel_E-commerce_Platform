package dan.edu.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:notification-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"notification.jwt.issuer=http://user-service:8080",
		"notification.jwt.audience=quickbite-api",
		"notification.jwt.secret-base64=dXNlci1zZXJ2aWNlLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcyE="
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
