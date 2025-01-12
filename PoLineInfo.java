package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author shaofeng Guo
 * @Date 2022/7/6 11:16
 * @description: TODO
 **/
@Data
@TableName("po_line_info")
public class PoLineInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    // po_line的编号
    private String poLineId;
    // 所属po的编号
    private String poId;
    // 每个po行需要验收的数量
    private BigDecimal poLineNumber;
    // 品类名称
    @TableField(exist = false)
    private String categoryName;
    // 每个po行需要验收物品单位
    private String unit;
    // 每个po行需要验收物品单价（含税）
    @TableField("unit_price")
    private BigDecimal unitPrice;
    // 每个po行需要验收物品单价（不含税）
    @TableField("uninclude_unit_price")
    private BigDecimal unincludeUnitPrice;
    // po行含税总金额
    @TableField("sum_amount")
    private BigDecimal sumAmount;
    // po行不含税总金额
    @TableField("uninclude_sum_amount")
    private BigDecimal unincludeSumAmount;
    // po行验收数量
    private BigDecimal acceptanceQty;

    // po行已验收数量
    @TableField(exist = false)
    private BigDecimal acceptanceQtyed;

    @TableField(exist = false)
    private BigDecimal acceptedQty;
    // po行已验收金额（含税）
    private BigDecimal acceptanceAmount;

    // po行已验收金额（含税）
    @TableField(exist = false)
    private BigDecimal acceptanceAmounted;

    // po行已验收金额（不含税）
    private BigDecimal unincludeAcceptanceAmount;
    // po行描述信息
    private String description;
    // 创建时间
    private Date createdTime;
    // 更新时间
    private Date updatedTime;
    // 税率
    @TableField("tax_rate")
    private BigDecimal taxRate;
    // 逻辑删除状态
    @TableField("delete_status")
    private Integer deleteStatus;
    // 剩余可验收金额(含税)
    @TableField(exist = false)
    private BigDecimal acceptedAmount;
    // 币种
    @TableField(exist = false)
    private String currency;

    @TableField(exist = false)
    private BigDecimal sumPrice;
}
