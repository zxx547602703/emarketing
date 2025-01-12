package com.wiko.emarket.controller;

import com.alibaba.fastjson.JSONObject;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.entity.CountryQuarterBudgetEntity;
import com.wiko.emarket.service.campaign.CountryQuarterBudgetService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/5/5 13:55
 */
@RestController
@RequestMapping("/api")
public class CountryQuarterBudgetController extends AbstractController {
    @Resource
    private CountryQuarterBudgetService service;
    @PostMapping("/country/quarter/budget")
    public R saveCountryQuarterBudget(@RequestBody CountryQuarterBudgetEntity entity){
        SysUserEntity user = this.getUser();
        String ret = service.saveCountryBudget(entity, user);
        if(!ret.equals("success")){
            return R.error(100,ret);
        }
        return R.ok();
    }

    @GetMapping("/country/quarter/budget/{code}/{year}")
    public JSONObject getCountryQuarterBudget(@PathVariable("code") String code,
                                              @PathVariable("year") String year){
        JSONObject countryQuarterBudget = service.getCountryQuarterBudget(code, year);
        return countryQuarterBudget;
    }
}
