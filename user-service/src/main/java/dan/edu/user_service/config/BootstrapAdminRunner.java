package dan.edu.user_service.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dan.edu.user_service.model.UserAccount;
import dan.edu.user_service.repository.UserRepository;
import dan.edu.user_service.utils.enums.Role;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public BootstrapAdminRunner(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${user-service.bootstrap-admin.email:}") String email,
			@Value("${user-service.bootstrap-admin.password:}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		boolean hasEmail = StringUtils.hasText(email);
		boolean hasPassword = StringUtils.hasText(password);
		if (!hasEmail && !hasPassword) {
			return;
		}
		if (hasEmail != hasPassword) {
			throw new IllegalStateException("Configure both INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD");
		}
		if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must be 12 to 72 UTF-8 bytes");
		}

		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		UserAccount admin = userRepository.findByEmail(normalizedEmail)
				.orElseGet(() -> userRepository.save(new UserAccount(
						normalizedEmail,
						passwordEncoder.encode(password),
						"Platform",
						"Administrator",
						null)));
		if (!admin.getRoles().contains(Role.ADMIN)) {
			admin.getRoles().add(Role.ADMIN);
		}
		admin.setEnabled(true);
	}
}
