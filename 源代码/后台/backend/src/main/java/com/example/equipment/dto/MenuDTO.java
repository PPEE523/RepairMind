package com.example.equipment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuDTO {

    private Long id;

    private String path;

    private String title;

    private String icon;

    private List<MenuDTO> children;
}
