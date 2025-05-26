package com.hina.log.domain.converter;

import com.hina.log.domain.dto.user.UserCreateDTO;
import com.hina.log.domain.dto.user.UserDTO;
import com.hina.log.domain.dto.user.UserUpdateDTO;
import com.hina.log.domain.entity.User;
import org.springframework.stereotype.Component;

/** 用户实体与DTO转换器 */
@Component
public class UserConverter implements Converter<User, UserDTO> {

    /** 将DTO转换为实体 */
    @Override
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        User entity = new User();
        entity.setId(dto.getId());
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setUid(dto.getUid());
        entity.setRole(dto.getRole());
        entity.setStatus(dto.getStatus());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        return entity;
    }

    /** 将创建DTO转换为实体 */
    public User toEntity(UserCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        User entity = new User();
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setStatus(1); // 默认启用

        return entity;
    }

    /** 将更新DTO转换为实体 */
    public User toEntity(UserUpdateDTO dto) {
        if (dto == null) {
            return null;
        }

        User entity = new User();
        entity.setId(dto.getId());
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setStatus(dto.getStatus());

        return entity;
    }

    /** 将实体转换为DTO */
    @Override
    public UserDTO toDto(User entity) {
        if (entity == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setNickname(entity.getNickname());
        dto.setEmail(entity.getEmail());
        dto.setUid(entity.getUid());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    /** 使用DTO更新实体 */
    @Override
    public User updateEntity(User entity, UserDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setStatus(dto.getStatus());

        return entity;
    }

    /** 使用创建DTO更新实体 */
    public User updateEntity(User entity, UserCreateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());

        return entity;
    }

    /** 使用更新DTO更新实体 */
    public User updateEntity(User entity, UserUpdateDTO dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole());
        entity.setStatus(dto.getStatus());

        return entity;
    }
}
