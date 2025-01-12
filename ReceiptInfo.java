package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/8/15 14:16
 */
@Data
@TableName("receipt_info")
public class ReceiptInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 发票凭证id
    private String receiptId;
    // 发票凭证年份
    private String year;
    // 冲销凭证号
    private String reversalVoucher;
    // 报销公司代码
    private String companyCode;
    // 供应商名称
    private String supplierName;
    // 供应商账户
    private String supplierAccount;
    // 开票总金额
    private BigDecimal sumAmount;
    // 会计期间(发票凭证的月份)
    private String month;
    // 货币码
    private String currency;
    // 落表时的汇率
    private BigDecimal exchangeRateUsd;
    // 税务代码
    private String taxCode;
    // 税额
    private BigDecimal taxAmount;
    // 税率
    private BigDecimal taxRate;
    // 状态
    private String status;
    // 创建人
    private String createdBy;
    // 创建时间
    private String createdTime;
    // 最后更新人
    private String updatedBy;
    // 最后更新时间
    private String updatedTime;
}
