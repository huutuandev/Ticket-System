package com.example.ticket.security.ratelimit;

import com.example.ticket.security.user.UserPrincipal;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String key = getClientIP(request);
        String role = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            key = principal.getId().toString();
            role = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("ROLE_USER");
        }

        Bucket bucket = rateLimitService.resolveBucket(key, role);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

            // Viết thẳng vào response, KHÔNG dùng sendError()
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write("""
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "You have exhausted your API Request Quota",
                    "retryAfter": %d
                }
                """.formatted(waitForRefill));
            response.getWriter().flush();
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
