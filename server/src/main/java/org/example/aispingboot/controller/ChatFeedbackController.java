package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ChatFeedbackDTO;
import org.example.aispingboot.DTO.response.ChatFeedbackResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.ChatFeedbackService;
import org.example.aispingboot.service.ConsentService;
import org.example.aispingboot.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatFeedbackController {
    private final ChatFeedbackService chatFeedbackService;
    private final ConsentService consentService;

    public ChatFeedbackController(ChatFeedbackService chatFeedbackService, ConsentService consentService) {
        this.chatFeedbackService = chatFeedbackService;
        this.consentService = consentService;
    }

    @PostMapping("/chat-feedback")
    public Result<Boolean> submit(@Valid @RequestBody ChatFeedbackDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        consentService.ensureConsented(userId);
        chatFeedbackService.create(userId, dto);
        return Result.ok(true, "感谢你的反馈");
    }

    @GetMapping("/admin/chat-feedback/page")
    public Result<Page<ChatFeedbackResponseDTO>> adminPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(chatFeedbackService.adminPage(page, pageSize));
    }
}
