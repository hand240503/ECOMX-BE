package com.ndh.ShopTechnology.services.task.impl;

import com.ndh.ShopTechnology.constants.OrderConstants;
import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.dto.request.task.*;
import com.ndh.ShopTechnology.dto.response.task.*;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.entities.task.*;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.enums.task.*;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.OrderRepository;
import com.ndh.ShopTechnology.repository.task.*;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.task.MentionParser;
import com.ndh.ShopTechnology.services.task.TaskNotificationService;
import com.ndh.ShopTechnology.services.task.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final KanbanBoardRepository boardRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskStatusHistoryRepository statusHistoryRepository;
    private final TaskActivityLogRepository activityLogRepository;
    private final TaskParticipantRepository participantRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final TaskMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PermissionService permissionService;
    private final TaskNotificationService notificationService;

    // ─── Board ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public KanbanBoardResponse getDefaultBoard() {
        KanbanBoardEntity board = boardRepository
            .findFirstByIsDefaultTrueAndIsActiveTrueAndIsDeletedFalse()
            .orElseGet(() -> {
                log.warn("No default Kanban board found — creating one on-the-fly");
                Long ownerId = userRepository.findOneByUsername("admin")
                    .map(u -> u.getId())
                    .orElse(1L);
                KanbanBoardEntity newBoard = KanbanBoardEntity.builder()
                    .name("Operations Board")
                    .description("Board quản lý công việc vận hành chính")
                    .ownerId(ownerId)
                    .isDefault(true)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
                return boardRepository.save(newBoard);
            });
        return getKanbanBoard(board.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public KanbanBoardResponse getKanbanBoard(Long boardId) {
        KanbanBoardEntity board = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundEntityException("Board khong ton tai: " + boardId));

        // Visibility: manager/order-permission → all tasks; staff → only assigned tasks
        List<TaskEntity> allTasks = resolveVisibleTasks(boardId);

        // Nhóm task theo status
        Map<TaskStatus, List<TaskResponse>> columns = Arrays.stream(TaskStatus.values())
            .collect(Collectors.toMap(
                s -> s,
                s -> new ArrayList<>(),
                (a, b) -> a,
                LinkedHashMap::new
            ));

        allTasks.forEach(t -> {
            TaskResponse resp = buildTaskResponse(t);
            columns.get(t.getStatus()).add(resp);
        });

        return KanbanBoardResponse.builder()
            .boardId(board.getId())
            .boardName(board.getName())
            .columns(columns)
            .stats(computeStats(boardId))
            .build();
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        UserEntity actor = getCurrentUser();

        int nextPosition = taskRepository
            .findMaxPositionByBoardIdAndStatus(request.getBoardId(), TaskStatus.NEW)
            .orElse(-1) + 1;

        UserEntity assignee = request.getAssigneeId() != null
            ? userRepository.findById(request.getAssigneeId()).orElse(null)
            : null;

        TaskEntity task = TaskEntity.builder()
            .boardId(request.getBoardId())
            .title(request.getTitle())
            .description(request.getDescription())
            .taskType(request.getTaskType())
            .departmentId(request.getDepartmentId())
            .priority(request.getPriority())
            .status(TaskStatus.NEW)
            .creator(actor)
            .assignee(assignee)
            .dueDate(request.getDueDate())
            .position(nextPosition)
            .sourceType(TaskSourceType.MANUAL)
            .isAutoGenerated(false)
            .build();

        task = taskRepository.save(task);

        // Ghi status history
        logStatusHistory(task, null, TaskStatus.NEW, actor, null);
        // Ghi activity log
        logActivity(task, actor, TaskActivityAction.CREATED,
            null, Map.of("title", task.getTitle(), "status", "NEW"), null);

        // Thêm participants nếu có
        if (request.getParticipantIds() != null) {
            addParticipants(task, request.getParticipantIds(), actor.getId());
        }

        log.info("Task created: id={}, title={}, creator={}", task.getId(), task.getTitle(), actor.getUsername());
        return buildTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        TaskEntity task = findActiveTask(taskId);
        return buildTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {
        UserEntity actor = getCurrentUser();
        TaskEntity task = findActiveTask(taskId);

        if (task.getCreator() != null && !task.getCreator().getId().equals(actor.getId())) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa task này vì bạn không phải là người tạo task.");
        }

        Map<String, Object> oldVal = new LinkedHashMap<>();
        Map<String, Object> newVal = new LinkedHashMap<>();

        if (request.getTitle() != null && !request.getTitle().equals(task.getTitle())) {
            oldVal.put("title", task.getTitle());
            newVal.put("title", request.getTitle());
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null && request.getPriority() != task.getPriority()) {
            oldVal.put("priority", task.getPriority());
            newVal.put("priority", request.getPriority());
            task.setPriority(request.getPriority());
        }
        if (request.getTaskType() != null) task.setTaskType(request.getTaskType());
        if (request.getDepartmentId() != null) {
            task.setDepartmentId(request.getDepartmentId() == -1L ? null : request.getDepartmentId());
        }
        if (request.getDueDate() != null) {
            oldVal.put("dueDate", task.getDueDate());
            newVal.put("dueDate", request.getDueDate());
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeId() != null) {
            UserEntity newAssignee = userRepository.findById(request.getAssigneeId()).orElse(null);
            oldVal.put("assigneeId", task.getAssignee() != null ? task.getAssignee().getId() : null);
            newVal.put("assigneeId", request.getAssigneeId());
            task.setAssignee(newAssignee);
        }

        task = taskRepository.save(task);

        if (!newVal.isEmpty()) {
            logActivity(task, actor, TaskActivityAction.TITLE_UPDATED, oldVal, newVal, null);
        }

        return buildTaskResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        UserEntity actor = getCurrentUser();
        TaskEntity task = findActiveTask(taskId);
        task.setIsDeleted(true);
        task.setDeletedAt(new Date());
        task.setDeletedBy(actor.getUsername());
        taskRepository.save(task);
        logActivity(task, actor, TaskActivityAction.DELETED, null,
            Map.of("taskId", taskId), null);
        log.info("Task soft-deleted: id={}, by={}", taskId, actor.getUsername());
    }

    // ─── Kanban Move ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse moveTask(Long taskId, MoveTaskRequest request) {
        UserEntity actor = getCurrentUser();
        TaskEntity task = findActiveTask(taskId);

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = request.getTargetStatus();

        // ── DONE lock: ORDER tasks can only be DONE 7 days after delivery ──
        if (newStatus == TaskStatus.DONE && task.getTaskType() == TaskType.ORDER_PREPARATION
                && task.getSourceId() != null) {
            orderRepository.findById(task.getSourceId()).ifPresent(order -> {
                if (order.getStatus() == null || order.getStatus() != OrderConstants.STATUS_COMPLETED) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Don hang chua duoc giao thanh cong, khong the dong task");
                }
                Date deliveredAt = order.getCompletedAt() != null ? order.getCompletedAt() : order.getModifiedDate();
                if (deliveredAt == null) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Khong xac dinh duoc ngay giao hang");
                }
                long daysSince = ChronoUnit.DAYS.between(deliveredAt.toInstant(), Instant.now());
                if (daysSince < 7) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Task chi duoc dong sau 7 ngay ke tu khi giao hang. Con " + (7 - daysSince) + " ngay.");
                }
            });
        }

        // Tính thời gian ở trạng thái cũ
        Integer minutesInPrev = computeMinutesInStatus(task.getId(), oldStatus);

        // Shift các task khác tại vị trí đích
        taskRepository.shiftPositionsDown(task.getBoardId(), newStatus, request.getTargetPosition());

        // Cập nhật trạng thái và vị trí
        task.setStatus(newStatus);
        task.setPosition(request.getTargetPosition());

        if (newStatus == TaskStatus.IN_PROGRESS && task.getStartedAt() == null) {
            task.setStartedAt(new Date());
        }
        if (newStatus == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(new Date());
        }

        task = taskRepository.save(task);

        // Ghi lịch sử
        logStatusHistory(task, oldStatus, newStatus, actor, minutesInPrev);
        logActivity(task, actor, TaskActivityAction.STATUS_CHANGED,
            Map.of("status", oldStatus.name()),
            Map.of("status", newStatus.name()),
            request.getNote() != null ? Map.of("note", request.getNote()) : null);

        notificationService.notifyStatusChanged(task, oldStatus.name(), newStatus.name(),
            actor, getParticipants(task));

        log.info("Task moved: id={} {} → {} pos={}", taskId, oldStatus, newStatus, request.getTargetPosition());
        return buildTaskResponse(task);
    }

    // ─── Assign ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse assignTask(Long taskId, Long assigneeId) {
        UserEntity actor = getCurrentUser();
        // Only manager/order-permission can assign order tasks
        if (!canManageAllTasks(actor.getUsername())) {
            throw new CustomApiException(HttpStatus.FORBIDDEN,
                "Chi manager hoac nguoi co quyen quan ly don hang moi duoc gan task");
        }
        TaskEntity task = findActiveTask(taskId);
        UserEntity newAssignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new NotFoundEntityException("User khong ton tai: " + assigneeId));

        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        task.setAssignee(newAssignee);

        if (task.getStatus() == TaskStatus.NEW) {
            task.setStatus(TaskStatus.ASSIGNED);
            logStatusHistory(task, TaskStatus.NEW, TaskStatus.ASSIGNED, actor, null);
        }

        task = taskRepository.save(task);

        logActivity(task, actor, TaskActivityAction.ASSIGNEE_CHANGED,
            Map.of("assigneeId", oldAssigneeId != null ? oldAssigneeId : "none"),
            Map.of("assigneeId", assigneeId), null);

        notificationService.notifyAssigned(task, newAssignee, actor);

        return buildTaskResponse(task);
    }

    // ─── Auto-create from source ──────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse autoCreateFromSource(TaskSourceType sourceType, Long sourceId,
                                             Long boardId, String titleOverride,
                                             String descriptionOverride) {
        // Idempotency: return existing task if already created
        if (taskRepository.existsBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId)) {
            log.warn("Task already exists for source {} id={}", sourceType, sourceId);
            return taskRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId)
                .map(this::buildTaskResponse).orElseThrow();
        }

        Long targetBoard = boardId;
        if (targetBoard == null) {
            targetBoard = boardRepository.findFirstByIsDefaultTrueAndIsActiveTrueAndIsDeletedFalse()
                .map(b -> b.getId())
                .orElseThrow(() -> new NotFoundEntityException("Khong tim thay board mac dinh"));
        }

        // All auto-generated tasks start as NEW, awaiting manager assignment
        TaskStatus initialStatus = TaskStatus.NEW;

        int nextPos = taskRepository
            .findMaxPositionByBoardIdAndStatus(targetBoard, initialStatus)
            .orElse(-1) + 1;

        TaskType type = switch (sourceType) {
            case ORDER        -> TaskType.ORDER_PREPARATION;
            case RETURN       -> TaskType.RETURN_PROCESSING;
            case PRICE_CHANGE -> TaskType.PRICE_UPDATE;
            case PROMOTION    -> TaskType.PROMOTION_SETUP;
            default           -> TaskType.OTHER;
        };

        String title = titleOverride != null ? titleOverride
            : switch (sourceType) {
                case ORDER  -> "Chuan bi don hang #" + sourceId;
                case RETURN -> "Xu ly hoan tra #"    + sourceId;
                default     -> "Task tu dong #"      + sourceId;
            };

        UserEntity systemUser = resolveSystemUser();
        TaskEntity task = TaskEntity.builder()
            .boardId(targetBoard)
            .title(title)
            .description(descriptionOverride)
            .taskType(type)
            .priority(sourceType == TaskSourceType.ORDER ? TaskPriority.HIGH : TaskPriority.MEDIUM)
            .status(initialStatus)
            .creator(systemUser)
            .sourceType(sourceType)
            .sourceId(sourceId)
            .isAutoGenerated(true)
            .position(nextPos)
            .build();

        task = taskRepository.save(task);
        logStatusHistory(task, null, initialStatus, systemUser, null);
        logActivity(task, null, TaskActivityAction.CREATED,
            null, Map.of("source", sourceType.name(), "sourceId", sourceId), null);

        log.info("Auto-task created: source={} id={} taskId={} status=NEW (awaiting assignment)",
            sourceType, sourceId, task.getId());
        return buildTaskResponse(task);
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskCommentResponse addComment(Long taskId, AddCommentRequest request) {
        UserEntity actor = getCurrentUser();
        TaskEntity task = findActiveTask(taskId);

        TaskCommentEntity parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId()).orElse(null);
        }

        TaskCommentEntity comment = TaskCommentEntity.builder()
            .task(task)
            .author(actor)
            .content(request.getContent())
            .parent(parent)
            .build();
        comment = commentRepository.save(comment);

        // Ghi activity log
        logActivity(task, actor, TaskActivityAction.COMMENTED,
            null, Map.of("commentId", comment.getId()), null);

        // Xử lý @mention — parse username từ nội dung
        processMentions(comment, task, actor);

        // Thông báo cho participants còn lại
        List<UserEntity> participants = getParticipants(task);
        notificationService.notifyCommented(task, actor, participants);

        return TaskCommentResponse.fromEntity(comment);
    }

    /** Parse @username trong comment, tạo TaskMentionEntity và gửi notification */
    private void processMentions(TaskCommentEntity comment, TaskEntity task, UserEntity mentioner) {
        Set<String> usernames = MentionParser.extractUsernames(comment.getContent());
        if (usernames.isEmpty()) return;

        for (String username : usernames) {
            userRepository.findOneByUsername(username).ifPresent(mentioned -> {
                // Tránh mention trùng
                if (mentionRepository.existsByComment_IdAndMentionedUser_Id(comment.getId(), mentioned.getId())) return;

                mentionRepository.save(TaskMentionEntity.builder()
                    .comment(comment)
                    .mentionedUser(mentioned)
                    .build());

                notificationService.notifyMentioned(task, mentioned, mentioner);
                log.info("@mention: taskId={} commentId={} mentioned={}", task.getId(), comment.getId(), username);
            });
        }
    }

    private List<UserEntity> getParticipants(TaskEntity task) {
        List<UserEntity> result = new java.util.ArrayList<>();
        if (task.getAssignee() != null) result.add(task.getAssignee());
        if (task.getCreator() != null) result.add(task.getCreator());
        participantRepository.findByTask_Id(task.getId())
            .forEach(p -> result.add(p.getUser()));
        return result.stream().distinct().collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskCommentResponse> getComments(Long taskId, Pageable pageable) {
        findActiveTask(taskId);
        return commentRepository
            .findByTask_IdAndIsDeletedFalseAndParentIsNullOrderByCreatedDateAsc(taskId, pageable)
            .map(TaskCommentResponse::fromEntity);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        UserEntity currentUser = getCurrentUser();
        TaskCommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundEntityException("Comment không tồn tại: " + commentId));

        boolean isAdmin = canManageAllTasks(currentUser.getUsername());
        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());

        if (!isAdmin && !isAuthor) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa bình luận này");
        }

        if (!isAdmin && isAuthor) {
            long hoursSinceCreation = ChronoUnit.HOURS.between(comment.getCreatedDate().toInstant(), Instant.now());
            if (hoursSinceCreation > 24) {
                throw new CustomApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xóa bình luận của mình trong vòng 24 giờ sau khi đăng");
            }
        }

        comment.setIsDeleted(true);
        comment.setDeletedAt(new Date());
        commentRepository.save(comment);
    }

    // ─── Checklist ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse addChecklist(Long taskId, AddChecklistRequest request) {
        TaskEntity task = findActiveTask(taskId);

        TaskChecklistEntity checklist = TaskChecklistEntity.builder()
            .task(task)
            .title(request.getTitle())
            .position(task.getChecklists() != null ? task.getChecklists().size() : 0)
            .build();

        List<TaskChecklistItemEntity> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (int i = 0; i < request.getItems().size(); i++) {
                items.add(TaskChecklistItemEntity.builder()
                    .checklist(checklist)
                    .title(request.getItems().get(i))
                    .position(i)
                    .build());
            }
        }
        checklist.setItems(items);

        // Reload task
        task = findActiveTask(taskId);
        return buildTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse toggleChecklistItem(Long itemId) {
        // Implementation would load item, toggle isDone, save
        // Simplified for brevity
        return null;
    }

    // ─── Activity Log ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TaskActivityLogResponse> getActivityLog(Long taskId, Pageable pageable) {
        findActiveTask(taskId);
        return activityLogRepository.findByTask_IdOrderByCreatedAtDesc(taskId, pageable)
            .map(TaskActivityLogResponse::fromEntity);
    }

    // ─── Cancel by source ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelTaskBySource(TaskSourceType sourceType, Long sourceId) {
        taskRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(sourceType, sourceId)
            .ifPresentOrElse(task -> {
                if (task.getStatus() == TaskStatus.CANCELLED || task.getStatus() == TaskStatus.DONE) {
                    log.debug("Task {} already terminal ({}), skip cancel", task.getId(), task.getStatus());
                    return;
                }
                TaskStatus prevStatus = task.getStatus();
                Integer minutesInPrev = computeMinutesInStatus(task.getId(), prevStatus);

                task.setStatus(TaskStatus.CANCELLED);
                taskRepository.save(task);

                logStatusHistory(task, prevStatus, TaskStatus.CANCELLED, null, minutesInPrev);
                logActivity(task, null, TaskActivityAction.STATUS_CHANGED,
                    java.util.Map.of("status", prevStatus.name()),
                    java.util.Map.of("status", TaskStatus.CANCELLED.name(),
                                     "reason", "Đơn hàng #" + sourceId + " bị hủy"),
                    null);

                log.info("Auto-cancelled task {} (source={} id={}) due to order cancellation",
                    task.getId(), sourceType, sourceId);
            }, () -> log.debug("No active task found for source={} id={}, nothing to cancel", sourceType, sourceId));
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────


    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public KanbanBoardResponse.DashboardStats getDashboardStats(Long boardId) {
        return computeStats(boardId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private TaskEntity findActiveTask(Long taskId) {
        return taskRepository.findById(taskId)
            .filter(t -> !t.getIsDeleted())
            .orElseThrow(() -> new NotFoundEntityException("Task khong ton tai: " + taskId));
    }

    private UserEntity getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findOneByUsername(username)
            .orElseThrow(() -> new NotFoundEntityException("User khong ton tai: " + username));
    }

    /**
     * Check if the current user can see/manage ALL tasks.
     * Rules: SUPER_ADMIN, ADMIN, MANAGER roles OR has READ_ORDER / UPDATE_ORDER permission.
     */
    private boolean canManageAllTasks(String username) {
        try {
            return permissionService.hasAnyPermission(username,
                PermissionCode.READ_ALL, PermissionCode.UPDATE_ALL,
                PermissionCode.READ_ORDER, PermissionCode.UPDATE_ORDER);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Return the list of tasks visible to the current user for a given board.
     * Manager/order-permission → all tasks.
     * Staff → only tasks assigned to them.
     */
    private List<TaskEntity> resolveVisibleTasks(Long boardId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (canManageAllTasks(username)) {
                return taskRepository.findByBoardIdAndIsDeletedFalseOrderByStatusAscPositionAsc(boardId);
            }
            UserEntity current = getCurrentUser();
            java.util.List<Long> deptIds = current.getUserDepartments().stream()
                .map(ud -> ud.getDepartment().getId())
                .collect(java.util.stream.Collectors.toList());
            if (deptIds.isEmpty()) {
                deptIds.add(-1L);
            }
            return taskRepository.findVisibleTasksForUser(boardId, current.getId(), deptIds);
        } catch (Exception e) {
            // Fallback: if auth context unavailable (e.g., scheduled jobs), return all
            log.warn("Could not resolve user for visibility filter, returning all tasks: {}", e.getMessage());
            return taskRepository.findByBoardIdAndIsDeletedFalseOrderByStatusAscPositionAsc(boardId);
        }
    }

    /**
     * Lay system user (admin) de dung lam creator cho auto-generated tasks.
     * Fallback: lay user dau tien trong DB neu admin khong ton tai.
     */
    private UserEntity resolveSystemUser() {
        return userRepository.findOneByUsername("admin")
            .orElseGet(() -> {
                org.springframework.data.domain.Page<com.ndh.ShopTechnology.entities.user.UserEntity> page =
                    userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1));
                if (page.isEmpty()) {
                    throw new NotFoundEntityException("Khong tim thay user nao trong he thong de lam creator");
                }
                return page.getContent().get(0);
            });
    }

    private void logStatusHistory(TaskEntity task, TaskStatus from, TaskStatus to,
                                  UserEntity actor, Integer minutesInPrev) {
        // changedBy NOT NULL — fallback to system user when actor is null (auto-generated tasks)
        UserEntity changedBy = actor != null ? actor : resolveSystemUser();
        statusHistoryRepository.save(TaskStatusHistoryEntity.builder()
            .task(task)
            .fromStatus(from)
            .toStatus(to)
            .changedBy(changedBy)
            .timeInPrevStatusMinutes(minutesInPrev)
            .build());
    }

    private void logActivity(TaskEntity task, UserEntity actor, TaskActivityAction action,
                             Map<String, Object> oldVal, Map<String, Object> newVal,
                             Map<String, Object> meta) {
        // actor_id NOT NULL — fallback to system user when actor is null (auto-generated tasks)
        UserEntity actorResolved = actor != null ? actor : resolveSystemUser();
        activityLogRepository.save(TaskActivityLogEntity.builder()
            .task(task)
            .actor(actorResolved)
            .actionType(action)
            .oldValue(oldVal)
            .newValue(newVal)
            .build());
    }

    private Integer computeMinutesInStatus(Long taskId, TaskStatus status) {
        // Dùng findByTask_IdOrderByChangedAtAsc (method có sẵn trong repo)
        // Tìm lần cuối cùng task chuyển vào 'status', tính thời gian từ đó đến hiện tại
        List<TaskStatusHistoryEntity> history =
            statusHistoryRepository.findByTask_IdOrderByChangedAtAsc(taskId);
        // Tìm entry cuối cùng có toStatus = status
        TaskStatusHistoryEntity last = null;
        for (TaskStatusHistoryEntity h : history) {
            if (status.equals(h.getToStatus())) last = h;
        }
        if (last == null || last.getChangedAt() == null) return null;
        long ms = System.currentTimeMillis() - last.getChangedAt().getTime();
        return (int) (ms / 60000);
    }

    private void addParticipants(TaskEntity task, List<Long> participantIds, Long actorId) {
        participantIds.forEach(uid -> {
            if (uid.equals(actorId)) return;
            userRepository.findById(uid).ifPresent(user ->
                participantRepository.save(TaskParticipantEntity.builder()
                    .task(task)
                    .user(user)
                    .role(TaskParticipantRole.PARTICIPANT)
                    .build())
            );
        });
    }

    private TaskResponse buildTaskResponse(TaskEntity task) {
        TaskResponse resp = TaskResponse.fromEntity(task);

        if (task.getChecklists() != null) {
            int total = task.getChecklists().stream()
                .mapToInt(c -> c.getItems() != null ? c.getItems().size() : 0).sum();
            int done = task.getChecklists().stream()
                .mapToInt(c -> c.getItems() != null
                    ? (int) c.getItems().stream().filter(i -> Boolean.TRUE.equals(i.getIsDone())).count()
                    : 0).sum();
            resp.setChecklistSummary(TaskResponse.ChecklistSummary.builder().total(total).done(done).build());
        }

        resp.setCommentCount(commentRepository.countByTask_IdAndIsDeletedFalse(task.getId()));
        resp.setAttachmentCount(attachmentRepository.countByTask_IdAndIsDeletedFalse(task.getId()));
        return resp;
    }

    private KanbanBoardResponse.DashboardStats computeStats(Long boardId) {
        long total      = taskRepository.countByBoardIdAndIsDeletedFalse(boardId);
        long inProgress = taskRepository.countByBoardIdAndStatusAndIsDeletedFalse(boardId, TaskStatus.IN_PROGRESS);
        long review     = taskRepository.countByBoardIdAndStatusAndIsDeletedFalse(boardId, TaskStatus.REVIEW);
        long overdue    = taskRepository.countByBoardIdAndDueDateBeforeAndStatusNotInAndIsDeletedFalse(
                            boardId, new java.util.Date(),
                            java.util.List.of(TaskStatus.DONE, TaskStatus.CANCELLED));
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long completedToday = taskRepository.countCompletedTodayByBoardId(boardId, cal.getTime());
        return KanbanBoardResponse.DashboardStats.builder()
            .total((int) total)
            .inProgress((int) inProgress)
            .review((int) review)
            .overdue((int) overdue)
            .completedToday((int) completedToday)
            .build();
    }
}
