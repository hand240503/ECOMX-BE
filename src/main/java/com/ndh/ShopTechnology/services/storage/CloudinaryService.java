package com.ndh.ShopTechnology.services.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ndh.ShopTechnology.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:ecomx/avatars}")
    private String avatarFolder;

    public UploadResult uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File avatar không hợp lệ");
        }
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", avatarFolder,
                            "resource_type", "image"
                    )
            );

            String url = (String) res.get("secure_url");
            String publicId = (String) res.get("public_id");

            if (url == null || publicId == null) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload avatar thất bại");
            }

            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Failed to upload avatar to Cloudinary", e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể upload avatar. Vui lòng thử lại.");
        }
    }

    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset: {}", publicId, e);
        }
    }

    public record UploadResult(String url, String publicId) {
    }
}

