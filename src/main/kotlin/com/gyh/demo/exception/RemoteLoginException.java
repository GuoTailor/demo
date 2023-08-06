package com.gyh.demo.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * create by GYH on 2023/5/13
 */
public class RemoteLoginException extends AuthenticationException {
    public RemoteLoginException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RemoteLoginException(String msg) {
        super(msg);
    }
}
