package com.hina.log.mapper;

import com.hina.log.entity.Datasource;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 数据源Mapper接口
 */
@Mapper
public interface DatasourceMapper {

    @Insert("INSERT INTO datasource (name, type, description, ip, port, username, password, database, jdbc_params, create_time, update_time) " +
            "VALUES (#{name}, #{type}, #{description}, #{ip}, #{port}, #{username}, #{password}, #{database}, #{jdbcParams}, " +
            "NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Datasource datasource);


    @Update("UPDATE datasource SET name=#{name}, type=#{type}, description=#{description}, " +
            "ip=#{ip}, port=#{port}, username=#{username}, password=#{password}, " +
            "database=#{database}, jdbc_params=#{jdbcParams}, update_time=NOW() WHERE id=#{id}")
    int update(Datasource datasource);


    @Delete("DELETE FROM datasource WHERE id=#{id}")
    int deleteById(Long id);


    @Select("SELECT * FROM datasource WHERE id=#{id}")
    Datasource selectById(Long id);


    @Select("SELECT * FROM datasource WHERE name=#{name}")
    Datasource selectByName(String name);


    @Select("SELECT * FROM datasource")
    List<Datasource> selectAll();

}
