package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.SignatoryInfo;
import com.wiko.emarket.vo.SignatoryInfoVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SignatoryInfoDao extends BaseMapper<SignatoryInfo> {
    List<List<?>> queryData(Map<String,Object> map);

    List<SignatoryInfoVo> queryList(Map<String,Object> map);
}
