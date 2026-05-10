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

    @Value("${cloudinary.documents-folder:ecomx/documents}")
    private String documentsFolder;

    public UploadResult uploadAvatar(MultipartFile file) {
        requireNonEmptyFile(file, "File avatar không hợp lệ");
        return uploadImageToFolder(file, avatarFolder, "avatar", "Upload avatar thất bại", "Không thể upload avatar. Vui lòng thử lại.");
    }

    public UploadResult uploadDocument(MultipartFile file) {
        requireNonEmptyFile(file, "File tài liệu không hợp lệ");
        return uploadImageToFolder(file, documentsFolder, "document", "Upload file thất bại", "Không thể upload file. Vui lòng thử lại.");
    }

    private static void requireNonEmptyFile(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private UploadResult uploadImageToFolder(
            MultipartFile file,
            String folder,
            String logLabel,
            String nullResultMessage,
            String ioErrorMessage) {
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );

            String url = (String) res.get("secure_url");
            String publicId = (String) res.get("public_id");

            if (url == null || publicId == null) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, nullResultMessage);
            }

            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Failed to upload {} to Cloudinary", logLabel, e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, ioErrorMessage);
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

