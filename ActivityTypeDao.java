package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.ActivityType;
import com.wiko.emarket.vo.foreign.ActivityTypeQueryVo;
import com.wiko.emarket.vo.foreign.ActivityTypeResponseVo;
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
public interface ActivityTypeDao extends BaseMapper<ActivityType> {

    /**
     * 有条件的查询业务科目
     *
     * @param activityTypeQueryVo
     * @return
     */
    List<ActivityTypeResponseVo> queryActivityTypeByParam(ActivityTypeQueryVo activityTypeQueryVo);
}
