package com.example.equipment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AuditRequest {

    @NotNull(message = "案例ID不能为空")
    private Long knowledgeId;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String remark;

    private String title;

    private String content;

    private String symptom;

    private String solution;

    private String equipModel;
    
    /**
     * 要上传的知识库ID列表（审核通过时使用）
     */
    private List<String> datasetIds;
}
