package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.ReviewerInfo;
import com.wiko.emarket.vo.ReviewerConfigVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewerConfigDao extends BaseMapper<ReviewerInfo> {

    List<List<?>> queryPage(Map<String,Object> map);

    List<ReviewerConfigVo> queryList(Map<String,Object> map);
}
