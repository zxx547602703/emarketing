package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 活动信息表
 */
@TableName("signatory_info")
@Data
public class SignatoryInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type= IdType.AUTO )
    private String id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private String userId;

    /**
     * 科目二代码
     */
    @TableField("lv1_subject")
    private String lv1Subject;

    /**
     * 区域码
     */
    @TableField("area_code")
    private String areaCode;


    /**
     * 审批的上限额度
     *
     */
    @TableField("max_amount")
    private BigDecimal maxAmount;


    /**
     * 创建时间
     */
    @TableField("created_date")
    private String createdDate;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新时间
     */
    @TableField("updated_date")
    private String updatedDate;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    @TableField(exist = false)
    private List<String> areaList;


    @TableField(exist = false)
    private List<String> subjectList;

}
