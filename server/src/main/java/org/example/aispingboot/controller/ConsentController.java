package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ConsentSubmitDTO;
import org.example.aispingboot.DTO.response.ConsentStatusResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.ConsentService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consents")
public class ConsentController {
    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/current")
    public Result<ConsentStatusResponseDTO> current() {
        return Result.ok(consentService.getStatus(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping
    public Result<ConsentStatusResponseDTO> submit(@Valid @RequestBody ConsentSubmitDTO dto) {
        return Result.ok(consentService.submit(SecurityUtil.getCurrentUserId(), dto), "同意确认成功");
    }

    @PostMapping("/revoke")
    public Result<ConsentStatusResponseDTO> revoke() {
        return Result.ok(consentService.revoke(SecurityUtil.getCurrentUserId()), "非必要授权已撤回");
    }
}
