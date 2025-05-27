package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.MachineInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 机器元信息Mapper接口 */
@Mapper
public interface MachineMapper {

    int insert(MachineInfo machineInfo);

    int update(MachineInfo machineInfo);

    int deleteById(Long id);

    MachineInfo selectById(Long id);

    MachineInfo selectByName(String name);

    List<MachineInfo> selectByIds(@Param("ids") List<Long> ids);

    List<MachineInfo> selectAll();
}
