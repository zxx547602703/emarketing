package com.wiko.emarket.service.campaign;


import com.baomidou.mybatisplus.service.IService;
import com.wiko.emarket.entity.FinanceInfo;

public interface FinanceInfoService extends IService<FinanceInfo> {

    /**
     * 根据活动id获取对应财经信息
     *
     * @param actId
     * @param refId
     * @return
     */
    public FinanceInfo getFinanceByActId(String actId, String refId);
}
