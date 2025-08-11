package com.hinadt.miaocha.ai.tool;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.domain.dto.module.ModuleInfoDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 模块管理工具类
 *
 * <p>提供AI助手调用的模块管理相关功能，包括获取所有模块信息、获取模块查询配置等功能。
 */
public class ModuleTool {

    private final ModuleInfoService moduleInfoService;

    public ModuleTool(ModuleInfoService moduleInfoService) {
        this.moduleInfoService = moduleInfoService;
    }

    /**
     * 获取所有模块信息
     *
     * @return 模块信息列表
     */
    @Tool(description = "获取系统中所有可用的模块信息列表，包括模块名称、数据源、表名、状态等详细信息。用于了解系统中有哪些日志模块可供查询。")
    public List<ModuleInfoDTO> getAllModules() {
        return moduleInfoService.getAllModules();
    }

    /**
     * 根据模块名称获取查询配置
     *
     * @param module 模块名称
     * @return 查询配置DTO，如果模块不存在或未配置则返回null
     */
    @Tool(description = "根据模块名称获取该模块的查询配置信息，包括时间字段、关键词检索字段配置、排除字段等。用于了解模块的查询能力和字段配置。")
    public QueryConfigDTO getQueryConfigByModule(
            @ToolParam(description = "模块名称，如 k8s-hina-cloud 等，用于获取对应模块的查询配置信息", required = true)
                    String module) {
        return moduleInfoService.getQueryConfigByModule(module);
    }
}
