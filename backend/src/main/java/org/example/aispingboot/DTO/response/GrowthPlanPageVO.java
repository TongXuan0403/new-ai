package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 成长计划分页返回
 */
@Data
@Builder
public class GrowthPlanPageVO {
    private List<GrowthPlanVO> records;
    private Long total;
}
