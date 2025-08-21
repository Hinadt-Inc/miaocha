package com.hinadt.miaocha.infrastructure.mapper;

import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 数据源Mapper接口 */
@Mapper
public interface DatasourceMapper {

    int insert(DatasourceInfo datasourceInfo);

    int update(DatasourceInfo datasourceInfo);

    int deleteById(Long id);

    int deleteAll();

    DatasourceInfo selectById(Long id);

    DatasourceInfo selectByName(String name);

    List<DatasourceInfo> selectAll();

    List<DatasourceInfo> selectByType(String type);

    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);
}
