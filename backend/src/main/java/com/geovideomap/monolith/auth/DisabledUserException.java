package com.geovideomap.monolith.auth;

import org.springframework.security.core.AuthenticationException;

public class DisabledUserException extends AuthenticationException {
    public DisabledUserException(String msg) {
        super(msg);
    }
}
