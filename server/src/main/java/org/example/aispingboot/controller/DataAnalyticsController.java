package org.example.aispingboot.controller;

import org.example.aispingboot.DTO.response.DataOverviewResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.DataAnalyticsService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/data-analytics")
public class DataAnalyticsController {
    private final DataAnalyticsService dataAnalyticsService;

    public DataAnalyticsController(DataAnalyticsService dataAnalyticsService) {
        this.dataAnalyticsService = dataAnalyticsService;
    }

    @GetMapping("/overview")
    public Result<DataOverviewResponseDTO> overview() {
        SecurityUtil.requireAdmin();
        return Result.ok(dataAnalyticsService.overview());
    }
}
