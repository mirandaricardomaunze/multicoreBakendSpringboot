package mz.multicore.erp.modules.activity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.activity.service.ActivityCapabilityPolicy;
import mz.multicore.erp.modules.activity.service.CompanyActivityService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Server-side check of the activity profile (SA-C01): the desktop hides sector features, and this refuses the
 * same requests from any other client. It runs after the security interceptor, which sets the company, and a
 * refusal is a {@code BusinessRuleException}, so the client gets the usual Portuguese message.
 */
@Configuration
public class ActivityCapabilityInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    /** After SecurityInterceptor and ClientVersionInterceptor, which register with the default order 0. */
    static final int ORDER = 100;

    private final CompanyActivityService activityService;

    public ActivityCapabilityInterceptor(CompanyActivityService activityService) {
        this.activityService = activityService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**").order(ORDER);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ActivityCapabilityPolicy.required(request.getMethod(), request.getRequestURI()).ifPresent(capability -> {
            Long companyId = CurrentUserContext.findCurrentCompanyId();
            if (companyId != null) activityService.requireCapability(companyId, capability);
        });
        return true;
    }
}
