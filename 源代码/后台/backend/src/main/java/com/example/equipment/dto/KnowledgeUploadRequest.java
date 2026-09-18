package com.example.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeUploadRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "故障描述不能为空")
    private String symptom;

    @NotBlank(message = "解决方案不能为空")
    private String solution;

    private String imgUrl;

    /**
     * 案例图片URL列表（JSON数组格式，最多5张）
     */
    private String images;

    @NotBlank(message = "设备型号不能为空")
    private String equipModel;
}
