package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.baomidou.mybatisplus.enums.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wiko.emarket.util.CampareFields;
import com.wiko.emarket.util.DifferentFields;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购信息表
 */
@TableName("purchase_info")
@Data
public class PurchaseInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value="uuid",type= IdType.UUID)
    private String uuid;

    /**
     * 采购方式
     */
    @TableField("purchase_type")
    @CampareFields()
    @DifferentFields()
    private String purchaseType;

    /**
     * 完成日期
     */
    @TableField("completed_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @CampareFields()
    private LocalDate completedDate;

    /**
     * 采购科目
     */
    @TableField("purchase_subject")
    @CampareFields()
    private String purchaseSubject;

    /**
     * 验收方式
     */
    @TableField("acceptance_type")
    private String acceptanceType;

    /**
     * 收货地址
     */
    @TableField(value = "address", strategy= FieldStrategy.IGNORED)
    private String address;

    /**
     * 验收要求
     */
    @TableField("acceptance_require")
    private String acceptanceRequire;

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

    @TableField("payment_type")
    private String paymentType;
}
