package com.ndh.ShopTechnology.entities.task;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity(name = "TaskAttachment")
@Table(name = "task_attachment",
    indexes = {
        @Index(name = "idx_ta_task",    columnList = "task_id"),
        @Index(name = "idx_ta_comment", columnList = "comment_id")
    })
public class TaskAttachmentEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ta_task"))
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", foreignKey = @ForeignKey(name = "fk_ta_comment"))
    private TaskCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ta_uploader"))
    private UserEntity uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 2048)
    private String filePath;

    @Column(name = "cloudinary_public_id", length = 512)
    private String cloudinaryPublicId;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
