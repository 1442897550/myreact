package com.xjtutjc.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SopSubmitResponseDTO {
    private boolean success;
    private String errorMessage;
    private String addId;
}
