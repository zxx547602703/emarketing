package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.TransferInfoVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransferInfoDao extends BaseMapper<TransferInfoVo> {

}
