package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.AttachmentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 附件Dao
 *
 */
@Mapper
public interface AttachmentInfoDao extends BaseMapper<AttachmentInfo> {
    /**
     * 查询附件
     *
     * @param refId
     * @return
     */
    List<AttachmentInfo> selectAttachmentById(@Param("refId") Integer refId);
}
