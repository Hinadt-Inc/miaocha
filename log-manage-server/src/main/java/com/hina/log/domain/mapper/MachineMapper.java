package com.hina.log.domain.mapper;

import com.hina.log.domain.entity.Machine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机器元信息Mapper接口
 */
@Mapper
public interface MachineMapper {

        int insert(Machine machine);

        int update(Machine machine);

        int deleteById(Long id);

        Machine selectById(Long id);

        Machine selectByName(String name);

        List<Machine> selectByIds(@Param("ids") List<Long> ids);

        List<Machine> selectAll();
}