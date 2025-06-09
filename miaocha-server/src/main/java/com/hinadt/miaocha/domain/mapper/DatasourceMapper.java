package com.hinadt.miaocha.domain.mapper;

import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 数据源Mapper接口 */
@Mapper
public interface DatasourceMapper {

    int insert(DatasourceInfo datasourceInfo);

    int update(DatasourceInfo datasourceInfo);

    int deleteById(Long id);

    DatasourceInfo selectById(Long id);

    DatasourceInfo selectByName(String name);

    List<DatasourceInfo> selectAll();
}
