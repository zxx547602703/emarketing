package com.wiko.emarket.entity;

import lombok.Data;

/**
 * 自行采购限制下拉选的科目VO
 *
 */
@Data
public class SelfPurchaseSubjectLimitVo {
    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典类型
     */
    private String type;

    /**
     * 字典码
     */
    private String code;

    /**
     * 字典值
     */
    private String valueCn;

    /**
     * 字典值(英文)
     */
    private String valueEn;

    /**
     * 上级code
     *
     */
    private String parentId;

    /**
     * 删除标记 -1：已删除 0：正常
     */
    private Integer delFlag;

    /**
     * 科目等级
     *
     */
    private String level;
}
