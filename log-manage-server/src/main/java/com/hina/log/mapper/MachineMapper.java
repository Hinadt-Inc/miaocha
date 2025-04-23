package com.hina.log.mapper;

import com.hina.log.entity.Machine;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 机器元信息Mapper接口
 */
@Mapper
public interface MachineMapper {

        @Insert("INSERT INTO machine (name, ip, port, username, password, ssh_key, create_time, update_time) " +
                        "VALUES (#{name}, #{ip}, #{port}, #{username}, #{password}, #{sshKey}, NOW(), NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Machine machine);

        @Update("UPDATE machine SET name=#{name}, ip=#{ip}, port=#{port}, username=#{username}, " +
                        "password=#{password}, ssh_key=#{sshKey}, update_time=NOW() WHERE id=#{id}")
        int update(Machine machine);

        @Delete("DELETE FROM machine WHERE id=#{id}")
        int deleteById(Long id);

        @Select("SELECT * FROM machine WHERE id=#{id}")
        Machine selectById(Long id);

        @Select("SELECT * FROM machine WHERE name=#{name}")
        Machine selectByName(String name);

        @Select("<script>SELECT * FROM machine WHERE id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
                        "</script>")
        List<Machine> selectByIds(@Param("ids") List<Long> ids);

        @Select("SELECT * FROM machine")
        List<Machine> selectAll();
}