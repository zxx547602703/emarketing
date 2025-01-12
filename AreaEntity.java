package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 18:27
 */
@Data
@TableName("area")
public class AreaEntity {
    @TableId(value = "id")
    private String id;
    @TableField("code")
    private String code;
    @TableField("name_cn")
    private String nameCn;
    @TableField("name_en")
    private String nameEn;
    @TableField("level")
    private String level;
    @TableField("parent_id")
    private String parentId;
    @TableField("created_date")
    private String createdDate;
    @TableField(exist = false)
    private List<AreaEntity> children;
}
