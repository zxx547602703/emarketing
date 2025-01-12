package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableLogic;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/26 16:56
 * @description: TODO
 **/
@TableName("benefit_prod")
@Data
public class BenefitProd {
    @TableId(type = IdType.INPUT)
    private String uuid;
    private String activityId;
    private String prodId;
    private String prodName;
    private BigDecimal ratio;
    @TableLogic(value = "1",delval = "0")
    private String status;
    private Date createdDate;
    private String createdBy;
    private Date updatedDate;
    private String updatedBy;
    private String refId;
}
