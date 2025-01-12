package com.wiko.emarket.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

/**
 * 活动信息表
 */
@TableName("sys_user_area")
@Data
public class SysUserArea {

    @TableId(value="id")
    private String id;

    @TableField("user_id")
    private Long userId;

    @TableField("level")
    private String level;

    @TableField("area_code")
    private String areaCode;

    @TableField("created_date")
    private String createdDate;

}
