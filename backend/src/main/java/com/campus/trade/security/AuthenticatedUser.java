package com.campus.trade.security;

public record AuthenticatedUser(Long userId, String role) {
}
