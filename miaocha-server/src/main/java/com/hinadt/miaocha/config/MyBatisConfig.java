package com.hinadt.miaocha.config;

import com.hinadt.miaocha.infrastructure.mybatis.UserAuditInterceptor;
import jakarta.annotation.PostConstruct;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/** MyBatis配置类 注册自定义拦截器和配置 */
@Configuration
public class MyBatisConfig {

    @Autowired private SqlSessionFactory sqlSessionFactory;

    @Autowired private UserAuditInterceptor userAuditInterceptor;

    /** 注册用户审计拦截器 */
    @PostConstruct
    public void addInterceptors() {
        sqlSessionFactory.getConfiguration().addInterceptor(userAuditInterceptor);
    }
}
