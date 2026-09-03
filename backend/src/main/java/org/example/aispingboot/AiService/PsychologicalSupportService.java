package org.example.aispingboot.AiService;

import org.example.aispingboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispingboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.KnowledgeBaseService;
import org.example.aispingboot.service.ConsultationMessageService;
import org.example.aispingboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {
    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        // 将初始用户消息保存到Message表
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

        // 创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, // 24小时
                1,
                "ACTIVE"
        );
    }

    public Flux<String> streamPsychologicalChat(Long userId, String sessionId, String userMessage) {
        Long dbSessionId = extractSessionId(sessionId);
        if (dbSessionId == null) {
            return Flux.error(new BusinessException("会话ID格式错误"));
        }
        consultationSessionService.validateSessionOwnership(dbSessionId, userId);

        // 创建响应流
        return Flux.create(sink -> {
            // 是否为初始消息
            boolean isInitialMessage = false;
            // 检查是否为初始消息，避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }
            if (!isInitialMessage) {
                // 保存用户消息到数据库
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            // 进行流式对话
            // 生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            // 构建系统提示词和知识库上下文
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);

            String knowledgeContext = knowledgeBaseService.buildKnowledgeContext(userMessage, 3);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT),
                    new SystemMessage(knowledgeContext)
            ));

            //用于存储AI完成的响应
            StringBuilder fullResponse = new StringBuilder();

            // 使用chatClient发送消息到Open AI
            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(Fragment -> {
                        fullResponse.append(Fragment);
                        sink.next(Fragment);
                    })
                    .doOnComplete(() -> {
                        String completeRes = fullResponse.toString();
                        // 将AI返回的内容保存到数据库
                        consultationMessageService.saveAimessage(dbSessionId, completeRes, "openai");
                        // 添加AI回复到chatMemory
                        List<Message> aiMessages = new ArrayList<>();
                        aiMessages.add(new AssistantMessage(completeRes));
                        chatMemory.add(conversationId, aiMessages);

                        sink.complete();
                    })
                    .doOnError(error -> {
                        // AI 服务不可用（如未配置有效 API Key）时，使用本地兜底回复，
                        // 保证前端 SSE 流程完整、消息可正常落库。
                        String fallback = buildLocalFallback(userMessage);
                        fullResponse.append(fallback);
                        sink.next(fallback);
                        consultationMessageService.saveAimessage(dbSessionId, fallback, "local-fallback");
                        List<Message> aiMessages = new ArrayList<>();
                        aiMessages.add(new AssistantMessage(fallback));
                        chatMemory.add(conversationId, aiMessages);
                        sink.complete();
                    })
                    .subscribe(); // 订阅并启动流
        });
    }

    // 获取参数中的sessionId
    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            String idStr = sessionId.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }

    /**
     * 本地兜底回复：当 AI 服务未配置有效 Key 或调用失败时使用，
     * 基于知识库上下文给出温暖、通用的心理支持内容。
     */
    private String buildLocalFallback(String userMessage) {
        String knowledgeHint = "";
        try {
            String context = knowledgeBaseService.buildKnowledgeContext(userMessage, 2);
            if (context != null && context.contains("知识库参考内容")) {
                knowledgeHint = "\n\n（以下建议参考了知识库中的相关文章内容）";
            }
        } catch (Exception ignore) {
            // 忽略知识库异常
        }
        return "我听到你的分享了，谢谢你对我的信任。你愿意把这件事说出来，本身就需要勇气。"
                + knowledgeHint
                + "\n\n如果此刻感觉比较沉重，可以先做几次深呼吸，让身体放松下来；也可以试着把脑海里的事情简单写下来，理一理头绪。"
                + "\n\n我在这里陪着你。如果愿意，可以继续和我聊聊细节，也可以告诉我你希望得到什么样的帮助。"
                + "\n\n（提示：当前后端未配置有效的 AI API Key，以上为本地演示回复。配置环境变量 AI_API_KEY 后即可接入 DeepSeek 智能对话。）";
    }
}
