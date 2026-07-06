package com.sungjiduk.backend.common.util;


import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;

public final class PreConditions {
    public static void validate(boolean expression, ErrorCode errorCode){
        if (!expression) throw new BusinessException(errorCode);
    }
}
