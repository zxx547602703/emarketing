package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.util.List;

/**
 * 活动信息表
 */
@TableName("approver_info")
@Data
public class ApproverInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type= IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private String userId;

    /**
     * 科目二代码
     */
    @TableField("lv2_subject")
    private String lv2Subject;

    /**
     * 区域码
     */
    @TableField("area_code")
    private String areaCode;


    /**
     * 来源
     */
    @TableField("source")
    private String source;


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

    public boolean notAllEq(ApproverInfo info){
        if(this.areaCode.equals(info.areaCode) && this.lv2Subject.equals(info.lv2Subject)
           && this.source.equals(info.source)){
            return true;
        }
        return false;
    }

    /**
     * 数据重复
     * @param info
     * @return
     */
    public boolean repeatVerification(ApproverInfo info){
        if(this.areaCode.equals(info.areaCode) && this.lv2Subject.equals(info.lv2Subject)
                && this.source.equals(info.source) && this.userId.equals(info.userId)){
            return true;
        }
        return false;
    }
}
