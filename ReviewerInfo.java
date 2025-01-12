package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

/**
 * 活动信息表
 */
@TableName("reviewer_info")
@Data
public class ReviewerInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type= IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 区域码
     */
    @TableField("area_code")
    private String areaCode;

    /**
     * 科目编码
     */
    @TableField("subject_code")
    private String subjectCode;


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


    public boolean notAllEquals(ReviewerInfo info){
        if (this.getAreaCode().equals(info.getAreaCode()) && this.getSubjectCode().equals(info.getSubjectCode())
                && this.getUserId().equals(info.getUserId())) {
            return true;
        } else {
            return false;
        }
    }

}
