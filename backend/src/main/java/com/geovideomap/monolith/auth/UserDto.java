package com.geovideomap.monolith.auth;

public record UserDto(Long id, String email, String displayName, String provider, String avatarUrl) {
    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getProvider(), u.getAvatarUrl());
    }
}
