package com.xjtutjc.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SopSearchResponseDTO {
    private List<SearchResponse> searchResponseList;
    private boolean success;
    private String errorMessage;
}
