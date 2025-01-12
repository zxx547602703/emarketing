package com.wiko.emarket.service.campaign;


import com.baomidou.mybatisplus.service.IService;
import com.wiko.emarket.entity.PurchaseInfo;

public interface PurchaseInfoService extends IService<PurchaseInfo> {

    /**
     * 根据活动id获取财经信息
     *
     * @param actId lv id
     * @param refId 来源id
     * @return
     */
    PurchaseInfo getPurchaseByActId(String actId, String refId);
}
