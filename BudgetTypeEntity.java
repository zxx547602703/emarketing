package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 18:27
 */
@Data
@TableName("budget_type")
public class BudgetTypeEntity {
    @TableId("code")
    private String code;
    @TableField("name_cn")
    private String nameCn;
    @TableField("name_en")
    private String nameEn;
    @TableField("source")
    private String source;
    @TableField("created_date")
    private String createdDate;
    /**
     * 预算分类中文名称
     */
    @TableField(exist = false)
    private String budgetName;
}
