package com.ndh.ShopTechnology.controller.document;

/**
 * Kiểm tra cặp tham số gắn document với thực thể khi upload multipart.
 */
public final class DocumentUploadRequestChecks {

    private DocumentUploadRequestChecks() {
    }

    /** {@code true} nếu chỉ một trong hai tham số được gửi — không hợp lệ. */
    public static boolean isIncompleteEntityPair(Long entityId, Integer entityType) {
        return entityId != null ^ entityType != null;
    }
}
