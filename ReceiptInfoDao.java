package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.ReceiptInfo;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReceiptInfoDao extends BaseMapper<ReceiptInfo> {
    Integer insertReceipt(ReceiptInfo info);
}

