package com.wiko.emarket.service.campaign;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.wiko.emarket.vo.CampaignCreateVo;
import com.wiko.emarket.vo.CampaignHistoryInfoVo;

import java.util.List;
import java.util.Map;

public interface CampaignService {
    /**
     * 提交
     *
     * @param campaignCreateVo
     * @return
     */
    R create(CampaignCreateVo campaignCreateVo);

    /**
     * 草稿保存
     *
     * @param campaignCreateVo
     * @return
     */
    R save(CampaignCreateVo campaignCreateVo);

    /**
     * 获取ActivityPid
     *
     * @param budgetCode
     * @return
     */
    List<String> getActivityPidByBudgetCode(String budgetCode);

    /**
     * 复制campaign
     * @param campaignId
     * @param id
     */
    CampaignHistoryInfoVo copyCampaign(String campaignId, Integer id);

    /**
     * 查询pr详情
     *
     * @param params 流程id
     */
    void getPrId(Map<String,Object> params);

    /**
     * 如果是变更则复制prID
     *
     * @param campaignId id
     */
    void isChangeCopyPr(String campaignId);

    /**
     * 删除campaign
     *
     * @param campaignId
     * @return
     */
    R deleteByCampaignId(String campaignId, Integer id);

    /**
     * 只更新campaign状态
     *
     * @param map
     * @return R
     */
    R getPrDetails(Map<String,Object> map);

    /**
     * 查询po详情
     *
     * @param params
     * @return R
     */
    R getPoDetails(Map<String, Object> params);

    /**
     * 获取可以转审的用户
     *
     * @param params
     * @return
     */
    PageUtils getTransferUsers(Map<String, Object> params);
}
