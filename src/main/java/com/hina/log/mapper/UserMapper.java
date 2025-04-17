package com.hina.log.mapper;

import com.hina.log.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id=#{id}")
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE uid=#{uid}")
    User selectByUid(String uid);

    @Insert("INSERT INTO user (nickname, email, uid, is_admin, create_time, update_time) " +
            "VALUES (#{nickname}, #{email}, #{uid}, #{isAdmin}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}