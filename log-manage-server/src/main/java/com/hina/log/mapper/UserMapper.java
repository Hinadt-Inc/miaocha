package com.hina.log.mapper;

import com.hina.log.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

        @Select("SELECT * FROM user WHERE id=#{id}")
        User selectById(Long id);

        @Select("SELECT * FROM user WHERE uid=#{uid}")
        User selectByUid(String uid);

        @Select("SELECT * FROM user WHERE email=#{email}")
        User selectByEmail(String email);

        @Select("SELECT * FROM user")
        List<User> selectAll();

        @Select("SELECT * FROM user WHERE role=#{role}")
        List<User> selectByRole(String role);

        /**
         * 根据ID列表批量查询用户
         *
         * @param ids 用户ID列表
         * @return 用户列表
         */
        @Select({ "<script>",
                        "SELECT * FROM user WHERE id IN",
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
                        "#{id}",
                        "</foreach>",
                        "</script>" })
        List<User> selectByIds(@Param("ids") List<Long> ids);

        @Insert("INSERT INTO user (nickname, email, uid, password, role, status, create_time, update_time) " +
                        "VALUES (#{nickname}, #{email}, #{uid}, #{password}, #{role}, #{status}, NOW(), NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(User user);

        @Update("UPDATE user SET nickname=#{nickname}, email=#{email}, role=#{role}, " +
                        "status=#{status}, update_time=NOW() WHERE id=#{id}")
        int update(User user);

        @Update("UPDATE user SET password=#{password}, update_time=NOW() WHERE id=#{id}")
        int updatePassword(Long id, String password);

        @Delete("DELETE FROM user WHERE id=#{id}")
        int deleteById(Long id);

        @Select("SELECT COUNT(*) FROM user WHERE role='SUPER_ADMIN'")
        int countSuperAdmins();
}