package com.wiko.emarket.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/28 15:41
 */
@Data
public class TypeAmountEntity {
    private String type;
    private BigDecimal amount;
    private String source;
}
