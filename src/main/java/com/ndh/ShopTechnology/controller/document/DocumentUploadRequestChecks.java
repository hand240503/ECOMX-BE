package com.ndh.ShopTechnology.controller.document;

public final class DocumentUploadRequestChecks {

    private DocumentUploadRequestChecks() {
    }

    public static boolean isIncompleteEntityPair(Long entityId, Integer entityType) {
        return entityId != null ^ entityType != null;
    }
}
