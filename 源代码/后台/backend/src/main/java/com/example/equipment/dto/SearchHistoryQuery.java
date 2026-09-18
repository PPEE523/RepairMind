package com.example.equipment.dto;

import lombok.Data;

@Data
public class SearchHistoryQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String equipModel;

    private Boolean searchSuccess;

    private String startTime;

    private String endTime;

    private String keyword;
}
