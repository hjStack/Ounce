package ounce.market.demo.funnel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ounce.market.demo.funnel.entity.FunnelEvent;
import ounce.market.demo.funnel.entity.FunnelEventType;
import ounce.market.demo.funnel.repository.FunnelEventRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class FunnelEventService {

    static final String SESSION_COOKIE = "ounce_session_id";
    private static final String SESSION_REQUEST_ATTRIBUTE = FunnelEventService.class.getName() + ".sessionId";

    private final FunnelEventRepository eventRepository;
    private final ObjectMapper objectMapper;


    public void recordFirstPageView(HttpServletRequest request, HttpServletResponse response,
                                    Long userId, Map<String, ?> metadata) {
        if (findSessionId(request) == null) {
            record(FunnelEventType.PAGE_VIEW, request, response, userId, metadata);
        }
    }

    /** 브라우저 쿠키를 기준으로 방문자를 식별하고, 이벤트를 저장한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(FunnelEventType type, HttpServletRequest request,
                       HttpServletResponse response, Long userId,
                       Map<String, ?> metadata) {
        String sessionId = findSessionId(request);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            request.setAttribute(SESSION_REQUEST_ATTRIBUTE, sessionId);
            response.addHeader("Set-Cookie", ResponseCookie.from(SESSION_COOKIE, sessionId)
                    .path("/")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(60L * 60 * 24 * 365)
                    .build().toString());
        }

        eventRepository.save(FunnelEvent.builder()
                .sessionId(sessionId)
                .userId(userId)
                .eventType(type.value())
                .metadata(toJson(metadata))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String findSessionId(HttpServletRequest request) {
        Object requestSessionId = request.getAttribute(SESSION_REQUEST_ATTRIBUTE);
        if (requestSessionId instanceof String value && !value.isBlank()) {
            return value;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String toJson(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("퍼널 이벤트 메타데이터를 JSON으로 변환할 수 없습니다.", e);
        }
    }
}
