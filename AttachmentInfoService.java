package com.wiko.emarket.service.campaign;


import com.baomidou.mybatisplus.service.IService;
import com.wiko.emarket.entity.AttachmentInfo;

import java.util.List;

public interface AttachmentInfoService extends IService<AttachmentInfo> {
    /**
     * 查询附件
     *
     * @param refId 来源id
     * @return 附件list
     */
    List<AttachmentInfo> queryByRefId(Integer refId);
}
