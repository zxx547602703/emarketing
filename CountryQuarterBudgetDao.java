package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.CountryQuarterBudgetEntity;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 14:55
 */
@Mapper
public interface CountryQuarterBudgetDao extends BaseMapper<CountryQuarterBudgetEntity> {
    BigDecimal getQuarterAmount(Map<String,String> map);
}
