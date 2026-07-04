package com.naon.grid.modules.app.exception;

import lombok.Getter;

@Getter
public class BindEmailRequiredException extends RuntimeException {

    private final String bindToken;

    public BindEmailRequiredException(String bindToken) {
        super("需要绑定邮箱");
        this.bindToken = bindToken;
    }
}
