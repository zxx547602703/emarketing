package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.wiko.emarket.dao.AttachmentInfoDao;
import com.wiko.emarket.entity.AttachmentInfo;
import com.wiko.emarket.service.campaign.AttachmentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttachmentInfoServiceImpl extends ServiceImpl<AttachmentInfoDao, AttachmentInfo> implements AttachmentInfoService {
    @Autowired
    AttachmentInfoDao attachmentInfoDao;


    @Override
    public List<AttachmentInfo> queryByRefId(Integer refId) {
        return baseMapper.selectAttachmentById(refId);
    }
}
