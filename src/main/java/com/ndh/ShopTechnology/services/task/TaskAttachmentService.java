package com.ndh.ShopTechnology.services.task;

import com.ndh.ShopTechnology.dto.response.task.TaskAttachmentResponse;
import com.ndh.ShopTechnology.entities.task.TaskAttachmentEntity;
import com.ndh.ShopTechnology.entities.task.TaskCommentEntity;
import com.ndh.ShopTechnology.entities.task.TaskEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.task.TaskAttachmentRepository;
import com.ndh.ShopTechnology.repository.task.TaskCommentRepository;
import com.ndh.ShopTechnology.repository.task.TaskParticipantRepository;
import com.ndh.ShopTechnology.repository.task.TaskRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final TaskActivityLogService activityLogService;
    private final TaskNotificationService notificationService;
    private final TaskParticipantRepository participantRepository;

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20 MB

    @Transactional
    public TaskAttachmentResponse uploadToTask(Long taskId, MultipartFile file, Long commentId) {
        TaskEntity task = findActiveTask(taskId);
        UserEntity uploader = getCurrentUser();

        validateFile(file);

        CloudinaryService.UploadResult result = cloudinaryService.uploadTaskAttachment(file, taskId);

        TaskCommentEntity comment = null;
        if (commentId != null) {
            comment = commentRepository.findById(commentId).orElse(null);
        }

        TaskAttachmentEntity attachment = TaskAttachmentEntity.builder()
            .task(task)
            .comment(comment)
            .uploader(uploader)
            .fileName(file.getOriginalFilename())
            .filePath(result.url())
            .cloudinaryPublicId(result.publicId())
            .fileSize(file.getSize())
            .mimeType(file.getContentType())
            .build();

        attachment = attachmentRepository.save(attachment);

        activityLogService.log(task, uploader,
            com.ndh.ShopTechnology.enums.task.TaskActivityAction.ATTACHMENT_ADDED,
            null,
            java.util.Map.of("fileName", file.getOriginalFilename(), "attachmentId", attachment.getId()),
            null);

        List<UserEntity> participants = getParticipants(task);
        notificationService.notifyAttachmentAdded(task, uploader, participants, file.getOriginalFilename());

        log.info("Attachment uploaded: taskId={}, file={}, by={}", taskId, file.getOriginalFilename(), uploader.getUsername());
        return TaskAttachmentResponse.fromEntity(attachment);
    }

    private List<UserEntity> getParticipants(TaskEntity task) {
        List<UserEntity> result = new java.util.ArrayList<>();
        if (task.getAssignee() != null) result.add(task.getAssignee());
        if (task.getCreator() != null) result.add(task.getCreator());
        participantRepository.findByTask_Id(task.getId())
            .forEach(p -> result.add(p.getUser()));
        return result.stream().distinct().collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getByTask(Long taskId) {
        findActiveTask(taskId);
        return attachmentRepository.findByTask_IdAndIsDeletedFalse(taskId)
            .stream().map(TaskAttachmentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long attachmentId) {
        TaskAttachmentEntity attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new NotFoundEntityException("Attachment không tồn tại: " + attachmentId));

        // Xóa khỏi Cloudinary
        cloudinaryService.deleteByPublicId(attachment.getCloudinaryPublicId());

        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);
        log.info("Attachment deleted: id={}", attachmentId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File không hợp lệ");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "File vượt quá giới hạn 20MB");
        }
    }

    private TaskEntity findActiveTask(Long taskId) {
        return taskRepository.findById(taskId)
            .filter(t -> !t.getIsDeleted())
            .orElseThrow(() -> new NotFoundEntityException("Task không tồn tại: " + taskId));
    }

    private UserEntity getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findOneByUsername(username)
            .orElseThrow(() -> new NotFoundEntityException("User không tồn tại"));
    }
}
