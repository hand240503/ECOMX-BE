package com.ndh.ShopTechnology.services.task;

import com.ndh.ShopTechnology.entities.task.TaskActivityLogEntity;
import com.ndh.ShopTechnology.entities.task.TaskEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.enums.task.TaskActivityAction;
import com.ndh.ShopTechnology.repository.task.TaskActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskActivityLogService {

    private final TaskActivityLogRepository activityLogRepository;

    public void log(TaskEntity task, UserEntity actor, TaskActivityAction action,
                    Map<String, Object> oldValue, Map<String, Object> newValue,
                    Map<String, Object> metadata) {
        if (actor == null) return;
        try {
            activityLogRepository.save(TaskActivityLogEntity.builder()
                .task(task)
                .actor(actor)
                .actionType(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .metadata(metadata)
                .createdAt(new Date())
                .build());
        } catch (Exception ex) {
            log.warn("Failed to write activity log for task {}: {}", task.getId(), ex.getMessage());
        }
    }
}
