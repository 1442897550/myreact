package com.xjtutjc.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponseDTO {
    private boolean success;
    private String answer;
    private String errorMessage;
}
