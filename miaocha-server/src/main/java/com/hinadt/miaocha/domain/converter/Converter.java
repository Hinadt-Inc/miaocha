package com.hinadt.miaocha.domain.converter;

/**
 * 转换器接口定义
 *
 * @param <E> 实体类型
 * @param <D> DTO类型
 */
public interface Converter<E, D> {

    /**
     * 将DTO转换为实体
     *
     * @param dto DTO对象
     * @return 实体对象
     */
    E toEntity(D dto);

    /**
     * 将实体转换为DTO
     *
     * @param entity 实体对象
     * @return DTO对象
     */
    D toDto(E entity);

    /**
     * 更新实体
     *
     * @param entity 需要更新的实体
     * @param dto 包含更新数据的DTO
     * @return 更新后的实体
     */
    E updateEntity(E entity, D dto);
}
