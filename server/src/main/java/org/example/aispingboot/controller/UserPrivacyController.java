package org.example.aispingboot.controller;

import org.example.aispingboot.DTO.response.PrivacyProfileResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.entity.UserDeletionRequest;
import org.example.aispingboot.service.UserPrivacyService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user/privacy")
public class UserPrivacyController {
    private final UserPrivacyService userPrivacyService;

    public UserPrivacyController(UserPrivacyService userPrivacyService) {
        this.userPrivacyService = userPrivacyService;
    }

    @GetMapping("/profile")
    public Result<PrivacyProfileResponseDTO> profile() {
        return Result.ok(userPrivacyService.getProfile(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/export")
    public Result<Map<String, String>> export() {
        String json = userPrivacyService.exportData(SecurityUtil.getCurrentUserId());
        return Result.ok(Map.of("content", json), "导出生成成功");
    }

    @PostMapping("/deletion-request")
    public Result<UserDeletionRequest> submitDeletion(@RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return Result.ok(userPrivacyService.submitDeletion(SecurityUtil.getCurrentUserId(), reason), "删除申请已提交");
    }

    @GetMapping("/deletion-request")
    public Result<UserDeletionRequest> deletionStatus() {
        return Result.ok(userPrivacyService.getDeletionStatus(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/deletion-request/cancel")
    public Result<Boolean> cancelDeletion() {
        boolean canceled = userPrivacyService.cancelDeletion(SecurityUtil.getCurrentUserId());
        return Result.ok(canceled, canceled ? "删除申请已取消" : "无进行中的删除申请");
    }
}
