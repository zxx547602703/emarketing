package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.dto.CArea;
import com.wiko.emarket.entity.SysUserArea;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:55
 */
@Mapper
public interface SysUserAreaDao extends BaseMapper<SysUserArea> {
    List<CArea> getAllCtyByUserId(String sysUserAreaCode);
}
