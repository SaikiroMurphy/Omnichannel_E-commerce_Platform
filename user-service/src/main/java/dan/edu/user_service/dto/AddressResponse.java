package dan.edu.user_service.dto;

public record AddressResponse(
		String label,
		String recipientName,
		String phone,
		String addressLine1,
		String addressLine2,
		String ward,
		String district,
		String city,
		String country,
		String postalCode) {
}
