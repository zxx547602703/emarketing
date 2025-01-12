package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.vo.ExchangeRateInfoVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExchangeRateInfoDao extends BaseMapper<ExchangeRateInfoVo> {
}
