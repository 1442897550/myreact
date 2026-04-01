package com.xjtutjc.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileUploadResponse {
    private String fileName;
    private String filePath;
    private Long fileSize;
}
