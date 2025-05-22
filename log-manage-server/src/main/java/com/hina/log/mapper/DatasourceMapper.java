package com.hina.log.mapper;

import com.hina.log.entity.Datasource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据源Mapper接口
 */
@Mapper
public interface DatasourceMapper {

    int insert(Datasource datasource);

    int update(Datasource datasource);

    int deleteById(Long id);

    Datasource selectById(Long id);

    Datasource selectByName(String name);

    List<Datasource> selectAll();

}
