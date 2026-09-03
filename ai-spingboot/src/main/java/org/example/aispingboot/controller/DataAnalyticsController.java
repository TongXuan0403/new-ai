package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.AnalyticsOverviewVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.DataAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalyticsController {
    @Resource
    private DataAnalyticsService dataAnalyticsService;

    @GetMapping("/overview")
    public Result<AnalyticsOverviewVO> overview() {
        return Result.ok(dataAnalyticsService.overview());
    }
}
