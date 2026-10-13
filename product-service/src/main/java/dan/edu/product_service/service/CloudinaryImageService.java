package dan.edu.product_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageService {

    private final Cloudinary cloudinary;

    public CloudinaryImageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is required");
        }

        if (image.getSize() > 8 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Image must not exceed 8MB");
        }

        String contentType = image.getContentType();
        if (contentType == null ||
                !contentType.matches("image/(jpeg|png|webp)")) {
            throw new IllegalArgumentException(
                    "Only JPEG, PNG and WebP images are allowed");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "omnichannel/products",
                            "resource_type", "image"
                    )
            );

            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload image to Cloudinary", e);
        }
    }
}
