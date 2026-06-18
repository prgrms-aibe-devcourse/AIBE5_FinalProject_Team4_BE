package com.closetnangam.be.global.auth.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final Long userId;
    private final boolean withdrawnRestoreRequired;

    public CustomOAuth2User(OAuth2User delegate, Long userId) {
        this(delegate, userId, false);
    }

    public CustomOAuth2User(OAuth2User delegate, Long userId, boolean withdrawnRestoreRequired) {
        this.delegate = delegate;
        this.userId = userId;
        this.withdrawnRestoreRequired = withdrawnRestoreRequired;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isWithdrawnRestoreRequired() {
        return withdrawnRestoreRequired;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
