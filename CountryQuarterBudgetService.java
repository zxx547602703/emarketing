package com.wiko.emarket.service.campaign;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.service.IService;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.entity.CountryQuarterBudgetEntity;

import java.math.BigDecimal;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/5/5 13:58
 */
public interface CountryQuarterBudgetService extends IService<CountryQuarterBudgetEntity> {
    String saveCountryBudget(CountryQuarterBudgetEntity entity, SysUserEntity user);

    BigDecimal getQuarterAmount(String code, String year);

    JSONObject getCountryQuarterBudget(String code, String year);
}
