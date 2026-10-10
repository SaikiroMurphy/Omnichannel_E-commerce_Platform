package dan.edu.user_service.dto;

import java.util.Set;

import dan.edu.user_service.utils.enums.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateRolesRequest(@NotEmpty Set<@NotNull Role> roles) {
}
