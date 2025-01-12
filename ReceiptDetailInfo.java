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
@TableName("receipt_detail_info")
public class    ReceiptDetailInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    // receipt_info中的id
    private Integer refId;
    // 发票凭证行号
    private String receiptLineId;
    //   采购凭证的项目编号 采购凭证号(po_id)
    private String refPoId;
    // 采购凭证的项目编号 (po_line表中的po_line_id)
    private String refPoLineId;
    // 物料号
    private String materialId;
    // 物料描述
    private String materialDescript;
    // 数量
    private BigDecimal qty;
    // 采购订单的计量单位
    private String unit;
    // 凭证货币金额（本币金额）
    private BigDecimal amount;
    // 订单类型（采购）
    private String orderType;
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
