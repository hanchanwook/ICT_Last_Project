package com.jakdang.labs.api.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_DIRECTOR("ROLE_DIRECTOR"),
    ROLE_STAFF("ROLE_STAFF"),
    ROLE_STUDENT("ROLE_STUDENT"),
    ROLE_INSTRUCTOR("ROLE_INSTRUCTOR");

    private final String role;

}