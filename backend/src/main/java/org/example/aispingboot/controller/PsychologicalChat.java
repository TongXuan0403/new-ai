package org.example.aispingboot.controller;

import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.aispingboot.AiService.PsychologicalSupportService;
import org.example.aispingboot.AiService.StructOutPut;
import org.example.aispingboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispingboot.DTO.command.ConsultationStreamDTO;
import org.example.aispingboot.DTO.response.SessionEmotionVO;
import org.example.aispingboot.DTO.response.SessionListItemVO;
import org.example.aispingboot.DTO.response.SessionMessageVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.service.ConsultationSessionQueryService;
import org.example.aispingboot.util.JwtTokenUtil;
import org.example.aispingboot.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;

    @Autowired
    private ConsultationSessionQueryService consultationSessionQueryService;

    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                    .build());
        }

        // 开始流式对话
        return psychologicalSupportService.streamPsychologicalChat(userId, streamDTO.getSessionId(), streamDTO.getUserMessage())
                .map(fragment -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", fragment, "type", "normal"))))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ))
                .delayElements(Duration.ofMillis(50)); // 添加延迟确保流式数据的体验
    }

    /**
     * 会话列表。管理员查看全部，普通用户查看自己的。
     */
    @GetMapping("/sessions")
    public Result<Page<SessionListItemVO>> sessions(
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        // 兼容前端两种分页参数命名：currentPage/size 与 pageNum/pageSize
        int page = (pageNum != null && pageNum > 0) ? pageNum : ((currentPage == null || currentPage < 1) ? 1 : currentPage);
        int pageSizeEffective = (pageSize != null && pageSize > 0) ? pageSize : ((size == null || size < 1) ? 10 : size);
        Long userId = UserContext.getCurrentUserId();
        boolean isAdmin = UserContext.isAdmin();
        return Result.ok(consultationSessionQueryService.listSessions(userId, isAdmin, page, pageSizeEffective));
    }

    /**
     * 会话消息列表
     */
    @GetMapping("/sessions/{id}/messages")
    public Result<List<SessionMessageVO>> messages(@PathVariable("id") Long sessionId) {
        Long userId = UserContext.getCurrentUserId();
        boolean isAdmin = UserContext.isAdmin();
        return Result.ok(consultationSessionQueryService.listMessages(sessionId, userId, isAdmin));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable("id") Long sessionId) {
        Long userId = UserContext.getCurrentUserId();
        boolean isAdmin = UserContext.isAdmin();
        consultationSessionQueryService.deleteSession(sessionId, userId, isAdmin);
        return Result.ok();
    }

    /**
     * 会话情绪分析
     */
    @GetMapping("/session/{id}/emotion")
    public Result<SessionEmotionVO> emotion(@PathVariable("id") String sessionIdRaw) {
        // 前端可能传 "session_1" 或 "1"，统一解析为数字 id
        Long sessionId = null;
        if (sessionIdRaw != null) {
            String s = sessionIdRaw.trim();
            if (s.startsWith("session_")) {
                s = s.substring("session_".length());
            }
            try {
                sessionId = Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new org.example.aispingboot.exception.BusinessException("会话ID格式错误");
            }
        }
        Long userId = UserContext.getCurrentUserId();
        boolean isAdmin = UserContext.isAdmin();
        return Result.ok(consultationSessionQueryService.analyzeEmotion(sessionId, userId, isAdmin));
    }
}
