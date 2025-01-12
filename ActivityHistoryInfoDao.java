package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.campaignReport.CampaignExcuteVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityHistoryInfoDao extends BaseMapper<ActivityHistoryInfoVo> {
    /**
     * campaign报表-执行明细查询sql
     *
     * @param map
     * @return
     */
    List<CampaignExcuteVo> queryCampaignReportPart(Map<String,Object> map);

}
