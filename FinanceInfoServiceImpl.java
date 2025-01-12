package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.wiko.emarket.dao.FinanceInfoDao;
import com.wiko.emarket.entity.FinanceInfo;
import com.wiko.emarket.service.campaign.FinanceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinanceInfoServiceImpl extends ServiceImpl<FinanceInfoDao, FinanceInfo> implements FinanceInfoService {

    @Autowired
    FinanceInfoDao financeInfoDao;

    @Override
    public FinanceInfo getFinanceByActId(String actId, String refId) {
        Wrapper<FinanceInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("activity_id", actId);
        wrapper.eq("ref_id", refId);
        wrapper.last("limit 1");
        return selectOne(wrapper);
    }
}
