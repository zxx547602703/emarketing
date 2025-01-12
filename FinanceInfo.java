package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wiko.emarket.util.CampareFields;
import com.wiko.emarket.util.DifferentFields;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财经信息表
 */
@TableName("finance_info")
@Data
public class FinanceInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value="uuid",type= IdType.UUID)
    private String uuid;

    /**
     * 来源：MSF、MBF
     */
    @TableField("source")
    @CampareFields()
    @DifferentFields()
    private String source;

    /**
     * 币种
     */
    @TableField("currency")
    @CampareFields()
    @DifferentFields()
    private String currency;

    /**
     * 支付公司
     */
    @TableField("payment_company")
    private String paymentCompany;

    /**
     * 申请金额
     */
    @TableField("apply_amount")
    @CampareFields()
    @DifferentFields()
    private BigDecimal applyAmount;

    /**
     * 申请金额(美元)
     */
    @TableField("apply_amount_usd")
    @CampareFields()
    @DifferentFields()
    private BigDecimal applyAmountUsd;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 对应activityL3的id
     */
    @TableField("activity_id")
    private String activityId;

    /**
     * 创建时间
     */
    @TableField("created_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 流程id
     */
    @TableField("ref_id")
    private String refId;
}
