package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.FormPoLineInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FormPoLineInfoDao extends BaseMapper<FormPoLineInfo> {
    List<FormPoLineInfo> selectFormPoLine(@Param("poId") String poId, @Param("poLineId") String poLineId);

    FormPoLineInfo selectFormPoLineByFormId(@Param("acceptanceFormId") String acceptanceFormId);

    /**
     * 查询需要从SAP获取采购收货参数的数据
     *
     * @return
     */
    List<FormPoLineInfo> querySapParams();
}
