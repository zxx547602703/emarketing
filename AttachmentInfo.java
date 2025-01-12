package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/29 10:26
 * @description: TODO
 **/
@TableName("attachment_info")
@Data
public class AttachmentInfo {
    @TableId
    private Integer attachmentId;
    private String attachmentName;
    @TableField(exist = false)
    private String name;
    private String filePath;
    private String refId;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date createdDate;
    private String createdBy;
}
