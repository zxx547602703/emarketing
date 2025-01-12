package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.AreaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:55
 */
@Mapper
public interface AreaDao extends BaseMapper<AreaEntity> {

    void deleteTreeNode(String parentId);

    List<AreaEntity> selectAuthAreaById(@Param("userId") Long userId);
}
