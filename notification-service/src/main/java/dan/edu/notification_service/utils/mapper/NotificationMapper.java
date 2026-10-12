package dan.edu.notification_service.utils.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import dan.edu.notification_service.dto.NotificationRequest;
import dan.edu.notification_service.dto.NotificationResponse;
import dan.edu.notification_service.model.Notification;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "status", ignore = true)
	Notification toEntity(NotificationRequest request);

	NotificationResponse toResponse(Notification entity);

	default List<NotificationResponse> toResponseList(List<Notification> entities) {
		return entities == null ? List.of() : entities.stream().map(this::toResponse).toList();
	}

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "status", ignore = true)
	void updateEntityFromRequest(NotificationRequest request, @MappingTarget Notification entity);
}
