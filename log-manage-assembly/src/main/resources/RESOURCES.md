# 资源文件管理说明

## 资源文件配置原则

本项目中，所有的配置文件（如 `.yml`, `.properties`, `.xml`）应当只在一个地方维护，以避免重复配置和潜在的不一致问题。

## 配置文件主要存放位置

- **主要位置**：`/home/ai/dev/ai_dev/log-manage-system/log-manage-server/src/main/resources`
- **次要位置**：`/home/ai/dev/ai_dev/log-manage-system/log-manage-assembly/src/main/resources`（仅保留 `version.txt`, `banner.txt` 等特殊文件）

## 打包和配置文件处理

在打包过程中：

1. `log-manage-assembly` 模块从 `log-manage-server` 模块中复制所有配置文件（`.yml`, `.xml`, `.properties`）
2. `log-manage-assembly` 模块中的 `version.txt` 和 `banner.txt` 等特殊文件会被保留
3. 其他配置文件应从 `log-manage-assembly/src/main/resources` 目录中移除，避免重复维护

## 开发者注意事项

- 如需修改配置，请只修改 `log-manage-server/src/main/resources` 中的文件
- 如果发现 `log-manage-assembly/src/main/resources` 中有重复的配置文件，可运行以下命令清理：
  ```
  mvn clean -Pclean-resources
  ```
- 配置更改后，确保应用正常启动并运行测试以验证更改 