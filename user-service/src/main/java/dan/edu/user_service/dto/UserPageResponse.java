package dan.edu.user_service.dto;

import java.util.List;

public record UserPageResponse(
		List<UserResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
