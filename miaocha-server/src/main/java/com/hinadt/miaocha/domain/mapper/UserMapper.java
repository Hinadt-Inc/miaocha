package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户Mapper接口 */
@Mapper
public interface UserMapper {

    User selectById(Long id);

    User selectByUid(String uid);

    /**
     * 根据uid获取用户邮箱（高效查询，仅返回email字段）
     *
     * @param uid 用户唯一标识符
     * @return 用户邮箱
     */
    String selectEmailByUid(String uid);

    /**
     * 根据邮箱获取用户昵称（高效查询，仅返回nickname字段）
     *
     * @param email 用户邮箱
     * @return 用户昵称
     */
    String selectNicknameByEmail(String email);

    User selectByEmail(String email);

    List<User> selectAll();

    List<User> selectByRole(String role);

    /**
     * 根据ID列表批量查询用户
     *
     * @param ids 用户ID列表
     * @return 用户列表
     */
    List<User> selectByIds(@Param("ids") List<Long> ids);

    int insert(User user);

    int update(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int deleteById(Long id);

    int countSuperAdmins();
}
