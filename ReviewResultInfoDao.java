package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.ReviewResultInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewResultInfoDao extends BaseMapper<ReviewResultInfo> {
    ReviewResultInfo selectByParams(@Param("formId") String formId,@Param("type") String type);
}
