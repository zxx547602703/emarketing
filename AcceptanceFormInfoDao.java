package com.wiko.emarket.dao;


import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.AcceptanceFormInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AcceptanceFormInfoDao extends BaseMapper<AcceptanceFormInfo> {

    AcceptanceFormInfo selectByFormId(@Param("formId") String formId);

    void updateApprover(AcceptanceFormInfo info);
}
