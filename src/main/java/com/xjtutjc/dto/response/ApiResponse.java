package com.xjtutjc.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApiResponse <T>{
    private int code;
    private String message;
    private T data;
}
