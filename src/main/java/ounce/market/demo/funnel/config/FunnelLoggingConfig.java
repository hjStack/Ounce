package ounce.market.demo.funnel.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ounce.market.demo.common.global.CustomUserDetails;
import ounce.market.demo.funnel.entity.FunnelEventType;
import ounce.market.demo.funnel.service.FunnelEventService;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class FunnelLoggingConfig implements WebMvcConfigurer {

    private final FunnelEventService eventService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) {
                if (isHtmlPage(request)) {
                    eventService.recordFirstPageView(request, response, currentUserId(),
                            Map.of("path", request.getRequestURI()));
                }
                return true;
            }
        }).addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/css/**", "/js/**", "/img/**", "/favicon*");
    }

    private boolean isHtmlPage(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && !request.getRequestURI().contains(".");
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.member().getMemberId();
        }
        return null;
    }
}
