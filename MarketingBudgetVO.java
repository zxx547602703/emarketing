package com.wiko.emarket.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author WIKO
 * @Date: 2022/4/27 - 04 - 27 - 11:34
 * @projectName:PSI
 * @Description: com.wiko.emarket.entiry
 */
public class MarketingBudgetVO {
    /**
     * 预算id
     */
    private String uuid;
    /**
     * 预算年份
     */
    private String year;
    /**
     * 预算级别
     */
    private String level;
    /**
     * 预算区域
     */
    private String areaCode;
    /**
     * 来源：MSF、MBF
     */
    private String source;
    /**
     * 预算分类
     */
    private String budgetType;
    /**
     * 预算状态
     */
    private Integer status;
    /**
     * 预算金额
     */
    private BigDecimal amount;
    /**
     * 预算父级id
     */
    private String parentId;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 更新人
     */
    private String updator;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 预算类别名称
     */
    private String budgetTypeName;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(String budgetType) {
        this.budgetType = budgetType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getUpdator() {
        return updator;
    }

    public void setUpdator(String updator) {
        this.updator = updator;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getBudgetTypeName() {
        return budgetTypeName;
    }

    public void setBudgetTypeName(String budgetTypeName) {
        this.budgetTypeName = budgetTypeName;
    }

    @Override
    public String toString() {
        return "MarketingBudgetVO{" +
                "uuid='" + uuid + '\'' +
                ", year='" + year + '\'' +
                ", level='" + level + '\'' +
                ", areaCode='" + areaCode + '\'' +
                ", source='" + source + '\'' +
                ", budgetType='" + budgetType + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", parentId='" + parentId + '\'' +
                ", createTime=" + createTime +
                ", creator='" + creator + '\'' +
                ", updator='" + updator + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}
