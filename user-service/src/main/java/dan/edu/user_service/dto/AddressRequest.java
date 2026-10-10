package dan.edu.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
		@Size(max = 60) String label,
		@NotBlank @Size(max = 160) String recipientName,
		@NotBlank @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,24}$") String phone,
		@NotBlank @Size(max = 200) String addressLine1,
		@Size(max = 200) String addressLine2,
		@Size(max = 100) String ward,
		@Size(max = 100) String district,
		@NotBlank @Size(max = 100) String city,
		@NotBlank @Size(max = 100) String country,
		@Size(max = 20) String postalCode) {
}
