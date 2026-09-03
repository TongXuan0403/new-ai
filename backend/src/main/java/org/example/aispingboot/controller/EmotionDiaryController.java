package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispingboot.DTO.response.EmotionDiaryPageVO;
import org.example.aispingboot.DTO.response.EmotionDiaryVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.EmotionDiaryService;
import org.example.aispingboot.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {
    @Resource
    private EmotionDiaryService emotionDiaryService;

    @PostMapping
    public Result<EmotionDiaryVO> add(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        String username = UserContext.getCurrentUsername();
        return Result.ok(emotionDiaryService.add(userId, username, dto));
    }

    @GetMapping("/admin/page")
    public Result<EmotionDiaryPageVO> adminPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String date) {
        return Result.ok(emotionDiaryService.adminPage(currentPage, size, keyword, date));
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        emotionDiaryService.delete(id);
        return Result.ok();
    }
}
