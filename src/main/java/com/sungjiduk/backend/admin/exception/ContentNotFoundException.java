package com.sungjiduk.backend.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContentNotFoundException extends RuntimeException {
    public ContentNotFoundException(Long id) {
        super("해당하는 작품을 찾을 수 없습니다." + id);
    }
}
