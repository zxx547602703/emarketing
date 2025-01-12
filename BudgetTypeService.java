package com.wiko.emarket.service.campaign;

import com.baomidou.mybatisplus.service.IService;
import com.wiko.emarket.entity.BudgetTypeEntity;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 15:50
 */
public interface BudgetTypeService extends IService<BudgetTypeEntity> {
    List<BudgetTypeEntity>  getBudgetType(String source, String areaCode);
}
