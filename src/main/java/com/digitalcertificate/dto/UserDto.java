package com.digitalcertificate.dto;

import com.digitalcertificate.entity.User;

public record UserDto(String name, String email, String role, String institution) {

    public static UserDto from(User u) {
        return new UserDto(u.getName(), u.getEmail(), u.getRole().name(),
                u.getInstitution() == null ? null : u.getInstitution().getName());
    }
}
