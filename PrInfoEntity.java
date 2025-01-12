package com.wiko.emarket.entity;

/**
 * @ClassName PrInfoEntity
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/20 9:33
 * @Version 1.0
 **/

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.util.Date;

/**
 * pr关联信息表
 * @author yanhui.zhao
 */
@TableName("pr_info")
@Data
public class PrInfoEntity {
    @TableField(exist = false)
    private String activityIdLv3;
    @TableField("pr_id")
    private String prId;
    @TableField("ref_activity_id")
    private Integer refActivityId;
    private String supplier;
    @TableField("created_time")
    private Date createTime;
    @TableField("updated_time")
    private Date updateTime;
    private Integer id;
    @TableField("delete_status")
    private Integer status;

    @TableField("pr_status")
    private String prStatus;
}
