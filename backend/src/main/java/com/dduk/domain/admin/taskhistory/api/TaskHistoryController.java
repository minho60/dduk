package com.dduk.domain.admin.taskhistory.api;

import com.dduk.domain.admin.taskhistory.service.TaskHistoryService;
import com.dduk.dto.admin.TaskHistoryDetailDto;
import com.dduk.dto.admin.TaskHistoryListDto;
import com.dduk.dto.common.ApiResponse;
import com.dduk.entity.admin.TaskHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
public class TaskHistoryController {

    private final TaskHistoryService taskHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskHistoryListDto>>> getTaskHistoryList(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TaskHistory> page = taskHistoryService.getTaskHistoryList(pageable);
        Page<TaskHistoryListDto> dtoPage = page.map(TaskHistoryListDto::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(dtoPage, "작업 이력 목록을 조회했습니다."));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskHistoryDetailDto>> getTaskHistoryDetail(@PathVariable String taskId) {
        TaskHistory taskHistory = taskHistoryService.getTaskHistoryDetail(taskId);
        if (taskHistory == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(TaskHistoryDetailDto.fromEntity(taskHistory), "작업 이력 상세를 조회했습니다."));
    }
}
