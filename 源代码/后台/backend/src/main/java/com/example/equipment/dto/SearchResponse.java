package com.example.equipment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {

    private String answer;

    private List<RetrieverResource> resources;

    private String conversationId;

    @Data
    @Builder
    public static class RetrieverResource {

        private String datasetId;

        private String datasetName;

        private String documentId;

        private String documentName;

        private String content;

        private Double score;

        private String segmentId;
    }
}
