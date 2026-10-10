package dan.edu.user_service.utils.mapper;

import java.util.List;

import dan.edu.user_service.dto.AddressResponse;
import dan.edu.user_service.dto.UserPageResponse;
import dan.edu.user_service.dto.UserResponse;
import dan.edu.user_service.model.Address;
import dan.edu.user_service.model.UserAccount;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

	UserResponse toResponse(UserAccount user);

	AddressResponse toResponse(Address address);

	List<UserResponse> toResponses(List<UserAccount> users);

	default UserPageResponse toPageResponse(Page<UserResponse> users) {
		return new UserPageResponse(
				users.getContent(),
				users.getNumber(),
				users.getSize(),
				users.getTotalElements(),
				users.getTotalPages());
	}
}
