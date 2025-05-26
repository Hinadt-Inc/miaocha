package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.Datasource;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 数据源Mapper接口 */
@Mapper
public interface DatasourceMapper {

    int insert(Datasource datasource);

    int update(Datasource datasource);

    int deleteById(Long id);

    Datasource selectById(Long id);

    Datasource selectByName(String name);

    List<Datasource> selectAll();
}
