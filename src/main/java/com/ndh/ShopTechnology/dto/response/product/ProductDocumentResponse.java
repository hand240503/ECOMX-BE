package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.constants.DocumentKind;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDocumentResponse {

    private Long id;
    private String fileName;
    /** URL đầy đủ (Cloudinary) hoặc path kiểu {@code /uploads/...}. */
    private String filePath;
    private String fileSize;
    /** {@link DocumentKind}: {@code 1} ảnh, {@code 2} video, {@code 3} tài liệu. */
    private Integer type;

    @JsonProperty("isMain")
    private Boolean main;

    public static ProductDocumentResponse fromEntity(DocumentEntity e) {
        if (e == null) {
            return null;
        }
        return ProductDocumentResponse.builder()
                .id(e.getId())
                .fileName(e.getFileName())
                .filePath(e.getFilePath())
                .fileSize(e.getFileSize())
                .type(e.getType())
                .main(e.isMain())
                .build();
    }
}
