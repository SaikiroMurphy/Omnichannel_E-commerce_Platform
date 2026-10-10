package dan.edu.user_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

	@Column(name = "address_label", length = 60)
	private String label;

	@Column(name = "address_recipient_name", length = 160)
	private String recipientName;

	@Column(name = "address_phone", length = 30)
	private String phone;

	@Column(name = "address_line1", length = 200)
	private String addressLine1;

	@Column(name = "address_line2", length = 200)
	private String addressLine2;

	@Column(name = "address_ward", length = 100)
	private String ward;

	@Column(name = "address_district", length = 100)
	private String district;

	@Column(name = "address_city", length = 100)
	private String city;

	@Column(name = "address_country", length = 100)
	private String country;

	@Column(name = "address_postal_code", length = 20)
	private String postalCode;

	protected Address() {
	}

	public Address(String label, String recipientName, String phone, String addressLine1,
			String addressLine2, String ward, String district, String city, String country, String postalCode) {
		this.label = trimToNull(label);
		this.recipientName = trimToNull(recipientName);
		this.phone = trimToNull(phone);
		this.addressLine1 = trimToNull(addressLine1);
		this.addressLine2 = trimToNull(addressLine2);
		this.ward = trimToNull(ward);
		this.district = trimToNull(district);
		this.city = trimToNull(city);
		this.country = trimToNull(country);
		this.postalCode = trimToNull(postalCode);
	}

	public String getLabel() {
		return label;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getPhone() {
		return phone;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public String getWard() {
		return ward;
	}

	public String getDistrict() {
		return district;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public String getPostalCode() {
		return postalCode;
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
