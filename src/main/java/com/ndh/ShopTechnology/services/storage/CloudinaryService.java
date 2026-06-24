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

    /**
     * Upload file đính kèm task — hỗ trợ cả image và raw file (pdf, docx, xlsx, ...).
     * Cloudinary resource_type="auto" tự phát hiện loại file.
     */
    public UploadResult uploadTaskAttachment(MultipartFile file, Long taskId) {
        requireNonEmptyFile(file, "File đính kèm không hợp lệ");
        String folder = "ecomx/task-attachments/" + taskId;
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            String url      = (String) res.get("secure_url");
            String publicId = (String) res.get("public_id");
            if (url == null || publicId == null) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload đính kèm thất bại");
            }
            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Failed to upload task attachment for task {}", taskId, e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể upload file đính kèm");
        }
    }

    /**
     * Upload ảnh / video bằng chứng trả hàng — resource_type="auto" để hỗ trợ cả video.
     * Trả thêm resourceType ("image" / "video" / "raw") để FE/BE phân loại media.
     */
    public ReturnMediaUploadResult uploadReturnMedia(MultipartFile file, Long orderId) {
        requireNonEmptyFile(file, "File bằng chứng trả hàng không hợp lệ");
        String folder = "ecomx/return-media/" + orderId;
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            String url          = (String) res.get("secure_url");
            String publicId     = (String) res.get("public_id");
            String resourceType = (String) res.get("resource_type");
            if (url == null || publicId == null) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload bằng chứng trả hàng thất bại");
            }
            return new ReturnMediaUploadResult(url, publicId, resourceType);
        } catch (IOException e) {
            log.error("Failed to upload return media for order {}", orderId, e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể upload bằng chứng trả hàng");
        }
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
        deleteByPublicId(publicId, "image");
    }

    /** Xoá asset Cloudinary theo resource_type ("image" / "video" / "raw"). */
    public void deleteByPublicId(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        String rt = (resourceType == null || resourceType.isBlank()) ? "image" : resourceType;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", rt));
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset: {} (resource_type={})", publicId, rt, e);
        }
    }

    public record UploadResult(String url, String publicId) {
    }

    /** Kết quả upload media trả hàng, kèm resourceType từ Cloudinary ("image" / "video" / "raw"). */
    public record ReturnMediaUploadResult(String url, String publicId, String resourceType) {
    }
}
