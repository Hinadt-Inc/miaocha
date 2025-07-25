# 秒查 SPI 规范模块

## 概述

`miaocha-spi` 是秒查系统的 SPI（Service Provider Interface）规范模块，定义了第三方扩展需要实现的接口和公共模型。通过这个模块，第三方开发者可以轻松地为秒查系统开发各种插件，而无需复制或重新定义接口和模型类。

## 模块结构

```
miaocha-spi/
├── src/main/java/com/hinadt/miaocha/spi/
│   ├── OAuthProvider.java              # OAuth 认证提供者接口
│   └── model/
│       └── OAuthUserInfo.java          # OAuth 用户信息模型
└── pom.xml
```

## 主要接口

### OAuthProvider

OAuth 认证提供者接口，用于实现第三方 OAuth 认证集成。

```java
public interface OAuthProvider {
    /**
     * 获取提供者名称
     * @return 提供者名称，如"Hinadt"
     */
    String getProviderName();

    /**
     * 验证认证凭据并获取用户信息
     * @param code 授权码
     * @param redirectUri 重定向URI
     * @return 用户信息对象
     */
    OAuthUserInfo authenticate(String code, String redirectUri);

    /**
     * 检查提供者是否可用
     * @return true表示可用，false表示不可用
     */
    default boolean isAvailable() {
        return true;
    }
}
```

## 主要模型

### OAuthUserInfo

OAuth 认证用户信息模型，包含用户的基本信息。

```java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {
    private String uid;          // 用户唯一标识符
    private String email;        // 用户邮箱（必填）
    private String nickname;     // 用户昵称
    private String realName;     // 用户真实姓名
    private String department;   // 用户部门
    private String position;     // 用户职位
}
```

## 如何使用

### 1. 添加依赖

在你的 SPI 实现项目的 `pom.xml` 中添加依赖：

```xml

<dependency>
    <groupId>com.hinadt.miaocha</groupId>
    <artifactId>miaocha-spi</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 实现接口

创建你的 OAuth 提供者实现：

```java
public class MyOAuthProvider implements OAuthProvider {
    @Override
    public String getProviderName() {
        return "MyProvider";
    }

    @Override
    public OAuthUserInfo authenticate(String code, String redirectUri) {
        // 实现你的认证逻辑
        // ...

        return OAuthUserInfo.builder()
                .uid("user123")
                .email("user@example.com")
                .nickname("用户昵称")
                .build();
    }

    @Override
    public boolean isAvailable() {
        // 检查配置是否正确，服务是否可用等
        return true;
    }
}
```

### 3. 配置 SPI

在你的项目的 `src/main/resources/META-INF/services/` 目录下创建文件 `com.hinadt.miaocha.spi.OAuthProvider`，内容为你的实现类的完整类名：

```
com.example.MyOAuthProvider
```

### 4. 打包和部署

将你的 SPI 实现打包成 JAR 文件，并放置在秒查系统的 classpath 中。系统会自动发现并加载你的实现。

## 注意事项

1. **线程安全**：你的实现类应该是线程安全的，因为可能会被多个线程同时调用。
2. **异常处理**：在 `authenticate` 方法中应该妥善处理异常，认证失败时返回 `null`。
3. **配置管理**：建议使用配置文件或环境变量来管理你的 OAuth 配置信息。
4. **日志记录**：建议添加适当的日志记录，便于调试和监控。

## 版本兼容性

- Java 17
- Spring Boot 3.x（如果你的实现中使用了 Spring 组件）
