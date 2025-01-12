package com.wiko.emarket.entity;

import lombok.Data;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/28 9:44
 */
@Data
public class CountryBudgetEntity {
    private String year;
    private String areaCode;
    private String ctyCode;
    private String source;
    private String level;
    private List<TypeAmountEntity> typeAmountEntities;
}
