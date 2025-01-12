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
 * @date: 2022/8/18 11:25
 */
@Data
@TableName("payment_voucher_Info")
public class PaymentVoucherInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 付款凭证编码
    private String paymentId;
    // 冲销凭证编码
    private String reversalVoucher;
    // 发票凭证编号
    private String receiptId;
    // 支付公司代码
    private String companyCode;
    // 会计年度
    private String year;
    // 供应商账号
    private String supplierAccount;
    // 供应商名称
    private String supplierName;
    // 币种
    private String currency;
    // 汇率, 付款日期对应的汇率
    private BigDecimal exchangeRateUsd;

    // 创建日期
    private String sapCreateDate;
    // 创建时间
    private String sapCreateTime;
    // 付款金额
    private BigDecimal paymentAmount;
    // 付款日期
    private String paymentDate;
    // 状态
    private String status;
    // 创建人
    private String createdBy;
    // 创建时间
    private String createdDate;
    // 最后更新人
    private String updatedBy;
    // 最后更新时间
    private String updatedDate;
}
