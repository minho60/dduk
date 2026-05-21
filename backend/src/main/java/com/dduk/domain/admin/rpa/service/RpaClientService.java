package com.dduk.domain.admin.rpa.service;

import com.dduk.domain.admin.taskhistory.service.TaskHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RpaClientService {

    private static final String DEFAULT_ACTION = "collect_purchase_orders";

    private final RestTemplate restTemplate;
    private final TaskHistoryService taskHistoryService;

    @Value("${rpa-server.url:http://localhost:5500}")
    private String rpaServerUrl;

    public RpaClientService(RestTemplateBuilder restTemplateBuilder, TaskHistoryService taskHistoryService) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.taskHistoryService = taskHistoryService;
    }

    @SuppressWarnings("unchecked")
    public String triggerRpaTask() {
        String taskId = "rpa-task-" + UUID.randomUUID().toString().substring(0, 8);
        String endpoint = rpaServerUrl + "/api/v1/rpa/trigger";
        log.info("[RPA Client] Triggering async RPA task: {} to endpoint: {}", taskId, endpoint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("taskId", taskId);
        requestBody.put("action", DEFAULT_ACTION);

        taskHistoryService.createRpaTriggerRequest(taskId, DEFAULT_ACTION, requestBody);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (responseEntity.getStatusCode() == HttpStatus.ACCEPTED || responseEntity.getStatusCode() == HttpStatus.OK) {
                taskHistoryService.markRpaTaskAccepted(taskId);
                log.info("[RPA Client] RPA trigger successfully accepted. Task ID: {}", taskId);
                return taskId;
            }

            taskHistoryService.markRpaTriggerFailure(
                    taskId,
                    DEFAULT_ACTION,
                    "Unexpected RPA trigger response status: " + responseEntity.getStatusCode(),
                    responseEntity.getBody()
            );
            log.warn("[RPA Client] RPA trigger response: {}", responseEntity.getStatusCode());
            return taskId;
        } catch (ResourceAccessException exception) {
            taskHistoryService.markRpaTriggerFailure(
                    taskId,
                    DEFAULT_ACTION,
                    "RPA server connection timeout",
                    Map.of("endpoint", endpoint)
            );
            log.error("[RPA Client] Connection timeout calling RPA server trigger: {}", exception.getMessage());
            throw new RuntimeException("RPA 서비스 연결이 지연되고 있습니다. 관리자에게 문의하세요.", exception);
        } catch (Exception exception) {
            taskHistoryService.markRpaTriggerFailure(
                    taskId,
                    DEFAULT_ACTION,
                    "RPA trigger failed",
                    Map.of("endpoint", endpoint)
            );
            log.error("[RPA Client] Failed to trigger RPA task", exception);
            throw new RuntimeException("RPA 연동 기동에 실패했습니다.", exception);
        }
    }
}
