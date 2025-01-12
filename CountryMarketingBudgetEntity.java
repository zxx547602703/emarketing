package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 14:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("marketing_budget")
public class CountryMarketingBudgetEntity {
    @TableId(value = "uuid",type = IdType.INPUT)
    private String uuid;
    @TableField("year")
    private String year;
    @TableField("level")
    private String level;
    @TableField("area_code")
    private String areaCode;
    @TableField("source")
    private String source;
    @TableField("budget_type_code")
    private String budgetTypeCode;
    @TableField("status")
    private String status;
    @TableField("amount")
    private BigDecimal amount;
    @TableField("parent_id")
    private String parentId;
    @TableField("created_date")
    private Date createdDate;
    @TableField("created_by")
    private String createdBy;
    @TableField("updated_date")
    private Date updatedDate;
    @TableField("updated_by")
    private String updatedBy;

    public boolean equals(CountryMarketingBudgetEntity entity){
        if(entity.getAmount().equals(this.amount) &&
            entity.getYear().equals(this.year)&&
                entity.getBudgetTypeCode().equals(this.budgetTypeCode)&&
                entity.getAreaCode().equals(this.areaCode)&&
                entity.getSource().equals(this.source)){
            return true;
        }
        return false;
    }
}
