package com.wiko.emarket.entity;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author shaofeng Guo
 * @Date 2022/7/11 14:39
 * @description: TODO
 **/
@Data
@TableName("review_result_info")
public class ReviewResultInfo {
    // 自增列
    private int id;
    // uuid
    private String reviewResultId;
    // 验收条目类型
    private String type;
    // 附件验收结果描述
    private String description;
    // 验收单号
    private String formId;

    /**
     * 创建时间
     */
    @TableField("created_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    @TableField(exist = false)
    private List<AttachmentInfo> fileList;
}
