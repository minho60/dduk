package com.dduk.repository.admin;

import com.dduk.entity.admin.TaskHistory;
import com.dduk.entity.admin.TaskHistoryStatus;
import com.dduk.entity.admin.TaskHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    Optional<TaskHistory> findByTaskId(String taskId);

    long countByTaskType(TaskHistoryType taskType);

    long countByTaskTypeAndStatus(TaskHistoryType taskType, TaskHistoryStatus status);

    long countByTaskTypeAndStatusIn(TaskHistoryType taskType, Collection<TaskHistoryStatus> statuses);

    long countByStatusAndCompletedAtAfter(TaskHistoryStatus status, LocalDateTime completedAt);
}
