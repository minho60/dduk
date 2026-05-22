package com.dduk.service.admin.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AiClientService {

    private final RestTemplate restTemplate;

    @Value("${ai-server.url:http://localhost:5000}")
    private String aiServerUrl;

    public AiClientService(RestTemplateBuilder restTemplateBuilder) {
        // AI 서비스에 맞춘 엄격한 타임아웃 격리: Connection 2초, Read 8초 (총 10초)
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> requestAiChat(String message, Object history) {
        String endpoint = aiServerUrl + "/api/v1/ai/chat";
        log.info("[AI Client] Sending request to AI server endpoint: {}", endpoint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("history", history);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(endpoint, requestEntity, Map.class);
            return (Map<String, Object>) responseEntity.getBody();
        } catch (ResourceAccessException e) {
            log.error("[AI Client] Timeout or connection failure to AI Server: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[AI Client] HTTP error response from AI Server: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("[AI Client] Unexpected error during AI service invocation", e);
            throw new RuntimeException("AI 서비스 연동에 실패했습니다.", e);
        }
    }
}
