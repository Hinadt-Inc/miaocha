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
