package com.geovideomap.monolith.auth;

public record LoginDto(String email, String password, boolean rememberMe) {}
