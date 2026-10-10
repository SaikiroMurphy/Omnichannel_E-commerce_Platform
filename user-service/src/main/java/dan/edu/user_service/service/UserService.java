package dan.edu.user_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dan.edu.user_service.common.ResourceNotFoundException;
import dan.edu.user_service.dto.AddressRequest;
import dan.edu.user_service.dto.AddressResponse;
import dan.edu.user_service.dto.UpdateProfileRequest;
import dan.edu.user_service.dto.UpdateRolesRequest;
import dan.edu.user_service.dto.UpdateStatusRequest;
import dan.edu.user_service.dto.UserResponse;
import dan.edu.user_service.model.Address;
import dan.edu.user_service.model.UserAccount;
import dan.edu.user_service.repository.UserRepository;
import dan.edu.user_service.utils.mapper.UserMapper;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@Transactional(readOnly = true)
	public UserResponse getProfile(UUID userId) {
		return userMapper.toResponse(findUser(userId));
	}

	@Transactional
	public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
		UserAccount user = findUser(userId);
		user.updateProfile(request);
		return userMapper.toResponse(user);
	}

	@Transactional(readOnly = true)
	public AddressResponse getAddress(UUID userId) {
		Address address = findUser(userId).getAddress();
		if (address == null) {
			throw new ResourceNotFoundException("Shipping address not found");
		}
		return userMapper.toResponse(address);
	}

	@Transactional
	public AddressResponse updateAddress(UUID userId, AddressRequest request) {
		UserAccount user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		user.updateAddress(new Address(
				request.label(),
				request.recipientName(),
				request.phone(),
				request.addressLine1(),
				request.addressLine2(),
				request.ward(),
				request.district(),
				request.city(),
				request.country(),
				request.postalCode()));
		return userMapper.toResponse(user.getAddress());
	}

	@Transactional
	public void removeAddress(UUID userId) {
		UserAccount user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		user.updateAddress(null);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(userMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(UUID userId) {
		return userMapper.toResponse(findUser(userId));
	}

	@Transactional
	public UserResponse updateRoles(UUID userId, UpdateRolesRequest request) {
		UserAccount user = findUser(userId);
		user.updateRoles(request.roles());
		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse updateStatus(UUID userId, UpdateStatusRequest request) {
		UserAccount user = findUser(userId);
		user.setEnabled(request.enabled());
		return userMapper.toResponse(user);
	}

	private UserAccount findUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}
}
