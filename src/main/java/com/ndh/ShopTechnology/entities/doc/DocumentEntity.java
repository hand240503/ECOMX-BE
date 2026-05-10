package com.ndh.ShopTechnology.entities.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "document")
public class DocumentEntity extends BaseEntity {

    public static final String COL_FILE_NAME            =   "file_name";
    public static final String COL_FILE_SIZE            =   "file_size";
    public static final String COL_FILE_PATH            =   "file_path";
    public static final String COL_TYPE                 =   "type";
    public static final String COL_FILE_DES             =   "file_des";
    public static final String COL_ENTITY_ID            =   "entity_id";
    public static final String COL_DESCRIPTION          =   "description";
    public static final String COL_ENTITY_TYPE          =   "entity_type";
    public static final String COL_CLOUDINARY_PUBLIC_ID =   "cloudinary_public_id";
    public static final String COL_IS_MAIN              =   "is_main";

    @Column(name = COL_FILE_NAME, nullable = true)
    private String      fileName;

    @Column(name = COL_FILE_SIZE, nullable = true)
    private String      fileSize;

    /** Với file local: {@code /uploads/yyMMdd/...}; với Cloudinary: {@code secure_url} đầy đủ. */
    @Column(name = COL_FILE_PATH, nullable = true, length = 2048)
    private String      filePath;

    /** Public id Cloudinary (để xóa asset); null nếu file lưu local trước đây. */
    @Column(name = COL_CLOUDINARY_PUBLIC_ID, length = 512)
    private String      cloudinaryPublicId;

    /**
     * Loại media — {@link com.ndh.ShopTechnology.constants.DocumentKind}: {@code 1} ảnh, {@code 2} video,
     * {@code 3} tài liệu. Giá trị {@code 0}: legacy chưa phân loại (coi như ảnh khi hiển thị gallery).
     */
    @Column(name = COL_TYPE, nullable = true)
    private int         type;

    /**
     * Ảnh hiển thị chính (cover) — <strong>chỉ có ý nghĩa khi {@link #type} = {@link com.ndh.ShopTechnology.constants.DocumentKind#IMAGE}</strong>
     * ({@code 1}). Với mỗi entity ({@code entity_id} + {@code entity_type}), trong dữ liệu chuẩn chỉ <strong>một</strong>
     * ảnh {@code is_main = true}; video/tài liệu không dùng làm cover.
     */
    @JsonProperty("isMain")
    @Column(name = COL_IS_MAIN, nullable = false)
    private boolean     main;

    @Column(name = COL_FILE_DES, nullable = true)
    private String      fileDes;

    @Column(name = COL_ENTITY_ID, nullable = true)
    private Long        entityId;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String      description;

    @Column(name = COL_ENTITY_TYPE, nullable = true)
    private Integer     entityType;
}
