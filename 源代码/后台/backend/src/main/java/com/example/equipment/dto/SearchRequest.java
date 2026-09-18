package com.example.equipment.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {

    private String query;

    private String equipModel;

    private List<String> fileUrls;

    private List<String> fileTypes;

    private List<String> fileNames;

    private Integer topK;

    private String agentType;
}
