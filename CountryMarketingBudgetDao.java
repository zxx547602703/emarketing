package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;
import com.wiko.emarket.vo.CampaignBudgetVo;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 14:55
 */
@Mapper
public interface CountryMarketingBudgetDao extends BaseMapper<CountryMarketingBudgetEntity> {
    BigDecimal getCountryBudgetSum(CountryMarketingBudgetEntity entity);

    BigDecimal getCountryBudgetSumByArea(Map<String, String> map);

    BigDecimal getCountryBudget(String uuid);

    List<CountryMarketingBudgetEntity> getAreaBudget(CountryMarketingBudgetEntity marketingBudgetEntity);

    BigDecimal calculateBenefitAmount(Map<String, String> map);

    List<CampaignBudgetVo> selectListGroupBy(Map<String, Object> map);
}
