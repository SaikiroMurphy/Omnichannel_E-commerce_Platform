package dan.edu.user_service.repository;

import java.util.Optional;
import java.util.UUID;

import dan.edu.user_service.model.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByEmail(String email);

	boolean existsByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select user from UserAccount user where user.id = :userId")
	Optional<UserAccount> findByIdForUpdate(@Param("userId") UUID userId);
}
