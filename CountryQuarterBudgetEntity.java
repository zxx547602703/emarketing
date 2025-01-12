package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/5/5 13:59
 */
@Data
@TableName("country_quarter_budget")
public class CountryQuarterBudgetEntity {
    @TableId(value = "uuid",type = IdType.INPUT)
    private String uuid;
    @TableField("year")
    private String year;
    @TableField("budget_id")
    private String budgetId;
    @TableField("quarter_code")
    private String quarterCode;
    @TableField("sum_amount")
    private BigDecimal sumAmount;
    @TableField("pre_amount")
    private BigDecimal preAmount;
    @TableField("area_code")
    private String areaCode;
    @TableField("status")
    private String status;
    @TableField("created_date")
    private Date createdDate;
    @TableField("created_by")
    private String createdBy;
    @TableField("updated_date")
    private Date updatedDate;
    @TableField("updated_by")
    private String updatedBy;
}
