package com.ndh.ShopTechnology.controller.admin.task;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.task.TaskAttachmentResponse;
import com.ndh.ShopTechnology.services.task.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/tasks")
@RequiredArgsConstructor
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    /**
     * Upload file đính kèm vào task.
     * Hỗ trợ đính kèm vào comment cụ thể qua query param commentId (tùy chọn).
     * Giới hạn 20 MB, tất cả loại file.
     */
    @PostMapping(value = "/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<TaskAttachmentResponse>> upload(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "commentId", required = false) Long commentId) {

        TaskAttachmentResponse result = attachmentService.uploadToTask(taskId, file, commentId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(APIResponse.of(true, "Upload thành công", result, null, null));
    }

    /** Lấy danh sách file đính kèm của task */
    @GetMapping("/{taskId}/attachments")
    public ResponseEntity<APIResponse<List<TaskAttachmentResponse>>> list(@PathVariable Long taskId) {
        return ResponseEntity.ok(
            APIResponse.of(true, "OK", attachmentService.getByTask(taskId), null, null));
    }

    /** Xóa file đính kèm (cả trên Cloudinary) */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<APIResponse<Void>> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xóa file đính kèm", null, null, null));
    }
}
