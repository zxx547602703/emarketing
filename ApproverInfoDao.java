package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.ApproverInfo;
import com.wiko.emarket.vo.ApproverInfoVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApproverInfoDao extends BaseMapper<ApproverInfo> {
    List<List<?>> queryPage(Map<String,Object> map);

    List<ApproverInfoVo> queryData(ApproverInfo info);

    List<ApproverInfoVo> queryList(Map<String,Object> map);
}
