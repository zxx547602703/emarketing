package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.FirstReviewerInfo;
import com.wiko.emarket.vo.FirstReviewerVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface FirstReviewerConfigDao extends BaseMapper<FirstReviewerInfo> {

    List<List<?>> queryPage(Map<String,Object> map);

    List<FirstReviewerVo> queryList(Map<String,Object> map);
}
