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
public class ProductDocumentSummary {

  private Long id;
  private String fileName;
  private String filePath;
  private String fileSize;
  private Integer type;

  @JsonProperty("isMain")
  private Boolean main;

  public static ProductDocumentSummary fromEntity(DocumentEntity d) {
    if (d == null) {
      return null;
    }
    return ProductDocumentSummary.builder()
        .id(d.getId())
        .fileName(d.getFileName())
        .filePath(d.getFilePath())
        .fileSize(d.getFileSize())
        .type(d.getType())
        .main(d.isMain())
        .build();
  }
}
