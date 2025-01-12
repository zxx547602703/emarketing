package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/27 10:21
 * @description: TODO
 **/
@TableName("activity_type")
@Data
public class ActivityType {
    @TableId(value="code", type= IdType.INPUT)
    private String code;
    @TableField("name_cn")
    private String nameCn;
    @TableField("name_en")
    private String nameEn;
    @TableField("parent_id")
    private String parentId;
    @TableField("level")
    private String level;
    @TableField("status")
    private String status;
    @TableField("created_date")
    private Date createdDate;
    @TableField("created_by")
    private String createdBy;
    @TableField("updated_date")
    private Date updatedDate;
    @TableField("updated_by")
    private String updatedBy;
    @TableField(exist = false)
    private List<ActivityType> children;
}
