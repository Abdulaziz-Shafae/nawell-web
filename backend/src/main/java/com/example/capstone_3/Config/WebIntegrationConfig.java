package com.example.capstone_3.Config;

import com.example.capstone_3.Model.*;
import com.example.capstone_3.Repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.HandlerInterceptor;

/** HTTP integration boundary. Existing controller paths and business services are preserved. */
@Configuration
@RequiredArgsConstructor
public class WebIntegrationConfig implements WebMvcConfigurer {
    private final AccountRepository accounts;
    private final EntityManager entities;
    @Value("${app.allowed-origins}") private String origins;

    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Requested-With").allowCredentials(true);
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                res.setHeader("X-Frame-Options", "DENY");
                res.setHeader("Cache-Control", "no-store");
                if ("OPTIONS".equals(req.getMethod())) return true;
                String path = req.getRequestURI().substring(req.getContextPath().length());
                boolean read = "GET".equals(req.getMethod());
                if (!read && !"XMLHttpRequest".equals(req.getHeader("X-Requested-With")))
                    return deny(res, 403, "Use the Nawall API client to submit this request");
                String origin = req.getHeader("Origin");
                if (!read && origin != null && Arrays.stream(origins.split(",")).noneMatch(origin::equals))
                    return deny(res, 403, "Request origin is not allowed");
                if (read && path.matches("/api/v1/media/images/[a-f0-9-]+\\.(png|jpg)")) return true;
                if (path.matches("/api/v1/account/(login|register/(individual|company)|verify-email)")) return true;
                if (read && (path.equals("/api/v1/health") || path.equals("/api/v1/skill/get") || path.matches("/api/v1/skill-offer/(available|skill/\\d+|provider/\\d+)")
                        || path.startsWith("/api/v1/search/") || path.matches("/api/v1/review/account/\\d+(/average)?"))) return true;
                Integer id = req.getSession(false) == null ? null : (Integer) req.getSession(false).getAttribute("accountId");
                Account account = id == null ? null : accounts.findAccountById(id);
                if (account == null) return deny(res, 401, "Please log in first");
                if (!"ACTIVE".equals(account.getStatus())) return deny(res, 403, "Account is not active");
                boolean admin = "ADMIN".equals(account.getAccountType());
                if (!admin && path.startsWith("/api/v1/skill-assessment/take/"))
                    return deny(res, 403, "Use the AI assessment flow to verify your skill");
                // Maintenance list/create/update/delete routes are privileged unless explicitly owned.
                String[] parts = path.split("/");
                if (parts.length >= 5) {
                    String resource = parts[3], action = parts[4];
                    boolean maintenance = action.equals("get") || action.equals("add") || action.equals("update") || action.equals("delete");
                    // Creating a review is a participant workflow, not generic maintenance.
                    if (resource.equals("review") && action.equals("add") && parts.length == 6) maintenance = false;
                    if (resource.equals("agreement") && action.equals("add")) maintenance = false;
                    if (maintenance && !admin) {
                        if (parts.length != 6 || !(action.equals("update") || action.equals("delete")))
                            return deny(res, 403, "Administrator access required");
                        int objectId;
                        try { objectId = Integer.parseInt(parts[5]); } catch (NumberFormatException ex) { return deny(res, 400, "Invalid record ID"); }
                        if (!owns(resource, objectId, id, action)) return deny(res, 403, "You cannot modify this record");
                    }
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
    private boolean owns(String resource, int objectId, Integer accountId, String action) {
        return switch (resource) {
            case "individual-profile", "company-profile" -> action.equals("update") && accountId.equals(objectId);
            case "skill-offer" -> { SkillOffer x = entities.find(SkillOffer.class, objectId); yield x != null && x.getProviderAccount() != null && accountId.equals(x.getProviderAccount().getId()); }
            case "learning-request" -> { LearningRequest x = entities.find(LearningRequest.class, objectId); yield x != null && x.getRequesterAccount() != null && accountId.equals(x.getRequesterAccount().getId()); }
            case "account-skill" -> { AccountSkill x = entities.find(AccountSkill.class, objectId); yield action.equals("delete") && x != null && x.getAccount() != null && accountId.equals(x.getAccount().getId()); }
            case "session" -> { Session x = entities.find(Session.class, objectId); yield x != null && x.getSkillOffer() != null && x.getSkillOffer().getProviderAccount() != null && accountId.equals(x.getSkillOffer().getProviderAccount().getId()); }
            case "agreement" -> { Exchange x = entities.find(Exchange.class, objectId); yield action.equals("update") && x != null && x.getLearningRequest() != null && (accountId.equals(x.getLearningRequest().getRequesterAccount().getId()) || accountId.equals(x.getLearningRequest().getProviderAccount().getId())); }
            default -> false;
        };
    }
    private boolean deny(HttpServletResponse res, int code, String message) throws java.io.IOException {
        res.setStatus(code); res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"" + message + "\"}"); return false;
    }
}
