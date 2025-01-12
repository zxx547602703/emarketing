package com.wiko.emarket.service.campaign;


import com.baomidou.mybatisplus.service.IService;
import com.framework.common.utils.R;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.CampaignCreateVo;

import java.util.List;

public interface ActivityHistoryInfoService extends IService<ActivityHistoryInfoVo> {

    /**
     * 通过campaignId查询LV信息
     *
     * @param campaignId campaignId
     * @param id         campaign主键id
     * @return
     */
    List<ActivityHistoryInfoVo> getActivityHisByCampaignId(String campaignId, Integer id);

    /**
     * 保存LV2、LV3数据
     *
     * @param info
     * @param action
     * @param isAddRecord
     * @return
     */
    R updateInfo(CampaignCreateVo info, String action, boolean isAddRecord);

    /**
     * 根据campaignId更新L2,L3状态
     *
     * @param campaignId
     * @param id         campaign主键id
     * @param status
     */
    void updateActivityStatusByCampaignId(String campaignId, Integer id, String status, String deleteStatusCode);
}
