package dan.edu.user_service.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import dan.edu.user_service.utils.enums.Role;

public record UserResponse(
		UUID id,
		String email,
		String firstName,
		String lastName,
		String phone,
		boolean enabled,
		Set<Role> roles,
		AddressResponse address,
		Instant createdAt,
		Instant updatedAt) {
}
