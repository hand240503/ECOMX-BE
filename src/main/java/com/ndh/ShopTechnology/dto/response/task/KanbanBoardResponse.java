package com.ndh.ShopTechnology.dto.response.task;

import com.ndh.ShopTechnology.enums.task.TaskStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data @Builder
public class KanbanBoardResponse {
    private Long boardId;
    private String boardName;
    /** Các cột, mỗi cột chứa danh sách task */
    private Map<TaskStatus, List<TaskResponse>> columns;
    private DashboardStats stats;

    @Data @Builder
    public static class DashboardStats {
        private long total;
        private long inProgress;
        private long review;
        private long overdue;
        private long completedToday;
    }
}
