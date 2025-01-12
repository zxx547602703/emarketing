package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Author shaofeng Guo
 * @Date 2022/7/6 11:27
 * @description: TODO
 **/
@Data
@TableName("po_info")
public class PoInfo {
    // po的编号
    private String poId;
    // po关联pr的编号
    private String prId;
    // po行的数量（一个po中多少个po行）
    private BigDecimal poQty;
    // 供应商名称
    private String supplier;
    // 描述
    @TableField(value = "description", strategy= FieldStrategy.IGNORED)
    private String description;
    // 币种
    @TableField(value = "currency", strategy= FieldStrategy.IGNORED)
    private String currency;

    @TableField(value = "exchange_rate_usd")
    private BigDecimal exchangeRateUsd;

    // 创建时间
    @TableField("created_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;
    // 更新时间
    @TableField("updated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
    // 逻辑删除状态
    @TableField("delete_status")
    private Integer deleteStatus;
    // po详情列表
    @TableField(exist = false)
    private List<PoLineInfo> poLineLists;

    // po状态
    @TableField(value = "po_status")
    private String poStatus;
}
