package com.wiko.emarket.controller;

import com.alibaba.fastjson.JSONArray;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.entity.CountryBudgetEntity;
import com.wiko.emarket.service.campaign.CountryMarketingBudgetService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 14:21
 */
@RestController
@RequestMapping("/api")
public class CountryMarketingBudgetController extends AbstractController {

    @Resource
    private CountryMarketingBudgetService service;
    @PostMapping("/country/budget")
    public R saveCountryBudget(@RequestBody CountryBudgetEntity entity){
        SysUserEntity user = this.getUser();
        String ret = service.saveCountryBudget(entity, user);
        if(!ret.equals("success")){
            return R.error(100,ret);
        }
        return R.ok();
    }

    @GetMapping("/country/budget/{code}/{year}")
    public JSONArray getCountryBudget(@PathVariable("code") String code, @PathVariable("year") String year){
        return service.getCountryBudget(code, year);
    }

    @PostMapping("/country/budget/del")
    public R delCountryBudget(@RequestBody Map<String, Object> map){
        String ret = null;
        try {
            ret = service.delCountryBudget(map);
            if(ret.equals("success")) {
                return R.ok();
            }else {
                return R.error(ret);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.error("delete data error");
    }


}
