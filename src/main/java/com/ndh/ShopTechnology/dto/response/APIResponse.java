package com.ndh.ShopTechnology.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<ErrorResponse> errors;
    private Object metadata;
    private String timestamp;

    public static <T> APIResponse<T> of(
            boolean success,
            String message,
            T data,
            List<ErrorResponse> errors,
            Object metadata) {

        return APIResponse.<T>builder()
                .success(success)
                .message(message)
                .data(data)
                .errors(errors)
                .metadata(metadata)
                .timestamp(getCurrentTimestamp())
                .build();
    }

    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public APIResponse<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, Object>();
        }
        if (this.metadata instanceof Map) {
            ((Map<String, Object>) this.metadata).put(key, value);
        }
        return this;
    }

    public APIResponse<T> withMetadata(Map<String, Object> metadata) {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, Object>();
        }
        if (this.metadata instanceof Map) {
            ((Map<String, Object>) this.metadata).putAll(metadata);
        }
        return this;
    }

}
