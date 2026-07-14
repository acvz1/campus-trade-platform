package com.campus.trade.common.annotation;

import com.campus.trade.common.constant.Constant;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AdminOnlyAspect {
    @Before("@within(com.campus.trade.common.annotation.AdminOnly) || "
            + "@annotation(com.campus.trade.common.annotation.AdminOnly)")
    public void verifyAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !hasAdminRole(authentication)) {
            throw new AccessDeniedException("仅管理员可访问");
        }
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(Constant.ROLE_PREFIX + Constant.ROLE_ADMIN)
                        || authority.equals(Constant.ROLE_PREFIX + Constant.ROLE_SUPER_ADMIN));
    }
}
