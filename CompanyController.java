package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.wiko.emarket.service.campaign.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @ClassName CompanyController
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/5 9:25
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/company")
public class CompanyController {
    @Autowired
    private CompanyService companyService;
    /**
     * 查询Campaign预算明细
     *
     * @return List
     */
    @RequestMapping("/queryComPanyList")
    public R queryComPanyList(){
        return R.ok().put("data",companyService.queryComPanyList());
    }
    /**
     * 查询成本中心
     *
     * @param params 查询入参
     * @return List
     */
    @RequestMapping("/queryCostCenter")
    public R queryCostCenter(@RequestParam Map<String,Object> params){
        if(params.get("companyCode")==null){
            return R.ok().put("data",null);
        }
        return R.ok().put("data",companyService.queryCostCenter(params));
    }
}
