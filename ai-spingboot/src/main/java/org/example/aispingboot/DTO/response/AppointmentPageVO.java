package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 预约分页返回
 */
@Data
@Builder
public class AppointmentPageVO {
    private List<AppointmentVO> records;
    private Long total;
}
