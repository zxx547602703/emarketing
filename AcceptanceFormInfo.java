package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 验收单信息
 * @author linjian
 * date: 2022/7/8
 */
@TableName("acceptance_form_info")
@Data
public class AcceptanceFormInfo {
    @TableId(value="id",type= IdType.AUTO)
    private Long id;
    @TableField("process_id")
    private int processId;
    @TableField("form_id")
    private String formId;
    @TableField("po_id")
    private String poId;
    @TableField("campaign_id")
    private String campaignId;
    @TableField("activity_id")
    private String activityId;
    @TableField("acceptance_type")
    private String acceptanceType;
    @TableField("acceptance_qty")
    private BigDecimal acceptanceQty;
    @TableField("acceptance_amount")
    private BigDecimal acceptanceAmount;
    @TableField("uninclude_acceptance_amount")
    private BigDecimal unincludeAcceptanceAmount;
    /**
     * 开始日期
     */
    @TableField("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    /**
     * 结束日期
     */
    @TableField("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String description;
    @TableField("first_reviewer")
    private String firstReviewer;
    @TableField("second_appover")
    private String secondAppover;
    private int score;
    private String status;
    // 验收单状态码
    @TableField(exist = false)
    private String statusCode;

    /**
     * 创建时间
     */
    @TableField("created_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    @TableField("exchange_rate")
    private BigDecimal exchangeRate;

    @TableField("campaign_unique_id")
    private Long campaignUniqueId;
    @TableField("delete_status")
    private String deleteStatus;

    private String prId;
}
