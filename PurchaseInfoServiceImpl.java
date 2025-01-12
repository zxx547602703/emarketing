package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.wiko.emarket.dao.PurchaseInfoDao;
import com.wiko.emarket.entity.PurchaseInfo;
import com.wiko.emarket.service.campaign.PurchaseInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PurchaseInfoServiceImpl extends ServiceImpl<PurchaseInfoDao, PurchaseInfo> implements PurchaseInfoService {

    @Autowired
    PurchaseInfoDao purchaseInfoDao;

    @Override
    public PurchaseInfo getPurchaseByActId(String actId, String refId) {
        Wrapper<PurchaseInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("activity_id", actId);
        wrapper.eq("ref_id", refId);
        wrapper.last("limit 1");
        return selectOne(wrapper);
    }
}
