package com.wiko.emarket.service.campaign;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.service.IService;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.entity.CountryBudgetEntity;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 15:50
 */
public interface CountryMarketingBudgetService extends IService<CountryMarketingBudgetEntity> {
    BigDecimal getCountryBudgetSum(CountryMarketingBudgetEntity entity);

    CountryMarketingBudgetEntity getCountryBudget(String uuid);

    List<CountryMarketingBudgetEntity> getAreaBudget(CountryMarketingBudgetEntity marketingBudgetEntity);

    String saveCountryBudget(CountryBudgetEntity countryBudgetEntity, SysUserEntity user);

    JSONArray getCountryBudget(String code, String year);

    String delCountryBudget(Map<String,Object> map);

}
