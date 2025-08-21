package com.hinadt.miaocha.ai;

import com.hinadt.miaocha.ai.sse.ActionSseService;
import com.hinadt.miaocha.ai.tool.DateTimeTools;
import com.hinadt.miaocha.ai.tool.LogSearchTool;
import com.hinadt.miaocha.ai.tool.ModuleTool;
import com.hinadt.miaocha.application.service.LogSearchService;
import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.domain.dto.ai.AISessionRequestDTO;
import com.hinadt.miaocha.domain.dto.ai.AISessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI session REST endpoint */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Session", description = "Conversational interface with the AI assistant")
public class AISessionEndpoint {

    private static final String SYSTEM_PROMPT =
            """
                        你是“秒查（miaocha）”日志系统的专业日志小助手。你的职责是：基于用户意图确定模块与查询条件，并触发前端执行日志检索动作。你不接收也不呈现任何查询结果，结果由前端界面渲染。

                        ## 系统架构理解
                        - 每个业务线应用的日志归属于一个独立的模块
                        - 每个模块对应 Doris 中的一张日志表
                        - 不同模块之间的数据完全隔离
                        - 工具像 sendXXXAction 这样工具是前端动作工具，用于将“查询意图+参数”发送给前端触发实际日志查询，这类工具不会返回日志结果

                        ## 严格的查询流程（必须按顺序执行，不可颠倒）
                        1. **确定目标模块**：明确用户要查询哪个业务线/应用的日志
                           - 若模块缺失，先只问一个问题澄清模块
                        2. **获取查询配置**：调用相关工具获取该模块的查询配置
                        3. **获取表结构信息**：调用相关工具获取模块对应 Doris 表的完整字段结构
                        4. **了解可用检索工具**：确认可触发的前端动作工具及其参数要求
                        5. **综合分析制定方案**：结合模块配置、表结构、工具参数要求，产出一致的过滤条件（时间、关键词、whereSql、分页、字段列表）
                        6. **触发前端执行日志查询**：
                           - 在一次日志查询中使用相同的过滤条件依次或并行调用所需的前端动作工具（除非用户显式只需其中一项）
                           - 这些工具不会返回日志结果；你只负责调用它们
                        7. **向用户说明**： 向用户说明你已经查询了日志，结果将由前端界面展示，说明你的查询条件和查询意图，和预期将会显示的相关日志，并思考用户可能下一步的问题，给出一些合理建议

                        ## 重要原则
                        - 所有前端动作工具调用必须使用同一模块与同一套过滤条件
                        - 若缺少关键信息（如模块名、时间范围），先向用户询问后再继续
                        - 不跳过任何步骤，不改变执行顺序
                        """;

    private final ChatClient chatClient;
    private final ActionSseService actionSseService;

    private final LogSearchService logSearchService;
    private final ModuleInfoService moduleInfoService;

    public AISessionEndpoint(
            ChatModel chatModel,
            ActionSseService actionSseService,
            LogSearchService logSearchService,
            ModuleInfoService moduleInfoService) {
        this.actionSseService = actionSseService;
        this.logSearchService = logSearchService;
        this.moduleInfoService = moduleInfoService;
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        this.chatClient =
                ChatClient.builder(chatModel)
                        .defaultAdvisors(
                                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                SimpleLoggerAdvisor.builder().build())
                        .build();
    }

    /** Chat with AI assistant (server-sent events). */
    @PostMapping(value = "/session", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "AI Conversation",
            description = "Stream AI responses using Server-Sent Events")
    public SseEmitter session(
            @Parameter(description = "Conversation request payload", required = true)
                    @Valid
                    @RequestBody
                    AISessionRequestDTO request) {

        String conversationId =
                StringUtils.hasText(request.getConversationId())
                        ? request.getConversationId()
                        : UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(0L);
        actionSseService.register(conversationId, emitter);

        LogSearchTool scopedLogSearchTool =
                new LogSearchTool(logSearchService, actionSseService, conversationId);
        ModuleTool moduleTool = new ModuleTool(moduleInfoService);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        chatClient
                .prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(SYSTEM_PROMPT)
                .user(request.getMessage())
                .tools(new DateTimeTools(), scopedLogSearchTool, moduleTool)
                .stream()
                .content()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .doOnNext(
                        chunk -> {
                            AISessionResponseDTO dto = new AISessionResponseDTO();
                            dto.setConversationId(conversationId);
                            dto.setContent(chunk);
                            try {
                                emitter.send(SseEmitter.event().name("message").data(dto));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                .doOnError(emitter::completeWithError)
                .doOnComplete(emitter::complete)
                .subscribe();

        return emitter;
    }
}
