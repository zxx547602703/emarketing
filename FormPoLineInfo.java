package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @Author shaofeng Guo
 * @Date 2022/7/11 14:39
 * @description: TODO
 **/
@Data
@TableName("form_po_line_info")
public class FormPoLineInfo {
    // 自增列
    @ApiModelProperty(value = "id")
    @TableId(value="id",type= IdType.AUTO)
    private int id;
    // 验收单号
    @TableField("ref_form_id")
    private String refFormId;
    // poLine号
    private String poLineId;
    @TableField("po_id")
    private String poId;

    private BigDecimal acceptanceQty;

    @TableField("acceptance_amount")
    private BigDecimal acceptanceAmount;

    @TableField("uninclude_acceptance_amount")
    private BigDecimal unincludeAcceptanceAmount;
    /**
     * 验收单号
     */
    @TableField("material_document_no")
    private String materialNo;

    /**
     * 验收年
     */
    @TableField("material_document_year")
    private String materialYear;
    /**
     * 创建时间
     */
    @TableField("created_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDate createdTime;

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
    private LocalDate updatedTime;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    @TableField(exist = false)
    private BigDecimal unincludeUnitPrice;

    @TableField(exist = false)
    private BigDecimal poLineNumber;
}
