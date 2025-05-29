# 部署目录路径规范化改进

## 背景

为了兼容 `deployDir` 不是绝对路径的情况，系统需要支持相对路径自动转换为用户家目录下的绝对路径，以规避权限问题。

## 问题描述

原有系统中，`deployDir` 配置可能是：
- 绝对路径：如 `/opt/logstash` 
- 相对路径：如 `logstash`

当使用相对路径时，可能会遇到权限问题，因为相对路径的解析位置不确定。

## 解决方案

### 路径规范化规则

1. **绝对路径**（以 `/` 开头）：保持不变
   - 示例：`/opt/logstash` → `/opt/logstash`

2. **相对路径**（不以 `/` 开头）：转换为用户家目录下的路径
   - 示例：用户 `a`，`deployDir` 为 `logstash` → `/home/a/logstash`

### 实现细节

#### 1. AbstractLogstashCommand 增强

```java
/**
 * 规范化部署目录路径
 * 如果deployDir不是绝对路径（不以/开头），则将其转换为用户家目录下的路径
 * 
 * @param machineInfo 机器信息，用于获取用户名
 * @return 规范化后的绝对路径
 */
protected String normalizeDeployDir(MachineInfo machineInfo) {
    if (deployDir.startsWith("/")) {
        // 已经是绝对路径，直接返回
        return deployDir;
    } else {
        // 相对路径，转换为用户家目录下的路径
        String username = machineInfo.getUsername();
        return String.format("/home/%s/%s", username, deployDir);
    }
}
```

#### 2. 更新 getProcessDirectory 方法

```java
/**
 * 获取Logstash进程目录
 * 使用规范化后的部署目录路径
 */
protected String getProcessDirectory(MachineInfo machineInfo) {
    String normalizedDeployDir = normalizeDeployDir(machineInfo);
    return String.format("%s/logstash-%d", normalizedDeployDir, processId);
}
```

#### 3. 更新所有命令类

所有继承自 `AbstractLogstashCommand` 的命令类都已更新，使用新的 `getProcessDirectory(MachineInfo machineInfo)` 方法：

- `CreateDirectoryCommand`
- `StartProcessCommand` 
- `DeleteProcessDirectoryCommand`
- `UpdateConfigCommand`
- `ModifySystemConfigCommand`
- `VerifyProcessCommand`
- `StopProcessCommand`
- `ExtractPackageCommand`
- `RefreshConfigCommand`
- `UploadPackageCommand`
- `CreateConfigCommand`

## 使用示例

### 配置示例

```yaml
# application.yml
logstash:
  deploy-dir: logstash  # 相对路径
```

### 运行时转换

当用户为 `appuser`，机器信息中 `username` 为 `appuser` 时：

```
原始配置: deployDir = "logstash"
规范化后: /home/appuser/logstash
进程目录: /home/appuser/logstash/logstash-123
```

### 绝对路径示例

```yaml
# application.yml  
logstash:
  deploy-dir: /opt/logstash  # 绝对路径
```

运行时保持不变：

```
原始配置: deployDir = "/opt/logstash"
规范化后: /opt/logstash
进程目录: /opt/logstash/logstash-123
```

## 优势

1. **权限安全**：相对路径自动转换到用户家目录，避免权限问题
2. **向后兼容**：绝对路径配置保持不变
3. **灵活配置**：支持两种路径配置方式
4. **统一处理**：所有命令类统一使用规范化路径

## 注意事项

1. 确保目标机器用户有家目录的读写权限
2. 相对路径转换依赖 `MachineInfo.getUsername()` 的正确性
3. 所有命令执行时都会自动应用路径规范化

## 测试建议

1. 测试绝对路径配置的兼容性
2. 测试相对路径的自动转换
3. 验证不同用户下的路径生成
4. 确认权限问题得到解决 