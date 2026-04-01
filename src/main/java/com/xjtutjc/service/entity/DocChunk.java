package com.xjtutjc.service.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor

public class DocChunk {
    private String content;
    private int startIndex;
    private int endIndex;
    private int chunkIndex;
    private String title;
}
