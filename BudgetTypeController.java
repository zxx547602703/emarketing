package com.wiko.emarket.controller;

import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.service.campaign.BudgetTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/5/11 16:49
 */
@RestController
@RequestMapping("/api")
public class BudgetTypeController {
    @Resource
    private BudgetTypeService service;

    @GetMapping("/budget/type")
    public List<BudgetTypeEntity> getBudgetType(@RequestParam("source") String source,@RequestParam("areaCode") String areaCode){
        List<BudgetTypeEntity> budgetType = service.getBudgetType(source, areaCode);
        return budgetType;
    }
}
