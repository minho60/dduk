package com.dduk.domain.admin.rpa.api;

import com.dduk.domain.admin.taskhistory.service.TaskHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/callbacks")
public class RpaCallbackController {

    private static final String DEFAULT_ACTION = "collect_purchase_orders";

    private final TaskHistoryService taskHistoryService;

    @Value("${rpa-server.callback-token}")
    private String expectedCallbackToken;

    @SuppressWarnings("unchecked")
    @PostMapping("/rpa")
    public ResponseEntity<Map<String, Object>> rpaCallback(
            @RequestHeader(value = "X-RPA-Token", required = false) String incomingToken,
            @RequestBody Map<String, Object> payload) {

        try {
            log.info("[RPA Callback] Received webhook callback from RPA agent.");

            if (incomingToken == null || !incomingToken.equals(expectedCallbackToken)) {
                log.warn("[RPA Callback] Unauthorized access attempt.");
                return errorResponse(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않거나 권한이 없습니다.", "RPA_UNAUTHORIZED_CALLBACK");
            }

            String taskId = (String) payload.get("taskId");
            String status = (String) payload.get("status");

            if (taskId == null || status == null) {
                return errorResponse(HttpStatus.BAD_REQUEST, "잘못된 요청 인자 형식입니다.", "INVALID_CALLBACK_PARAMETER");
            }

            log.info("[RPA Callback] Task ID: {}, Executed Status: {}", taskId, status);

            if ("success".equalsIgnoreCase(status)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                taskHistoryService.saveRpaCallbackResult(taskId, DEFAULT_ACTION, status, payload, null);
                log.info("[RPA Callback] Scraping success. Gathered details: {}", data);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "요청을 정상 처리했습니다.");
                return ResponseEntity.ok(response);
            }

            Map<String, Object> error = (Map<String, Object>) payload.get("error");
            taskHistoryService.saveRpaCallbackResult(taskId, DEFAULT_ACTION, status, payload, error);
            log.error("[RPA Callback] Task {} failed. Error details: {}", taskId, error);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "오류 로그 기록을 정상 처리했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            log.error("[RPA Callback] Unexpected processing error in callback handler", exception);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 처리 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR");
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message, String code) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
