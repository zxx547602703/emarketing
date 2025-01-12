package com.wiko.emarket.service.campaign;


import com.baomidou.mybatisplus.service.IService;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.vo.CampaignHistoryInfoVo;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CampaignHistoryInfoService extends IService<CampaignHistoryInfoVo> {
    /**
     * 查询Campaign历史版本详情
     *
     * @param campaignId String
     * @param id         String
     * @return CampaignHistoryInfoVo
     */
    CampaignHistoryInfoVo queryCampaignHisInfo(String detailScene, String campaignId, Integer id);

    /**
     * campaign执行明细-条件搜索
     *
     * @param params
     * @return
     */
    List<CampaignHistoryInfoVo> findList(Map<String, Object> params);

    /**
     * campaign查询-条件搜索
     *
     * @param params
     * @return
     */
    PageUtils list(Map<String, Object> params, Long userId);


    /**
     * campaign执行信息tab页查询
     *
     * @param params
     * @return
     */
    PageUtils queryCampaignExecuteInformation(Map<String, Object> params, Long userId);

    /**
     * campaign执行信息tab页导出
     *
     * @param params
     * @return
     */
    void exportCampaignExecuteInformation(Map<String, Object> params, Long userId, HttpServletResponse response) throws IOException;

    /**
     * 查询po 开票金额详情
     *
     * @param params
     * @return R
     */
    R getPoBillingedDetails(Map<String, Object> params);

    /**
     * 查询po 付款金额详情
     *
     * @param params
     * @return R
     */
    R getPoPaymentedDetails(Map<String, Object> params);

    /**
     * L2回冲更新逻辑
     *
     * @param params
     * @return R
     */
    R updateL2Recovery(Map<String, Object> params, SysUserEntity currentUser);

    /**
     * 根据campaign 查询LV2回冲金额等详情
     *
     * @param params
     * @return R
     */
    R getCampaignRecoveryDialogInfo(Map<String, Object> params, SysUserEntity currentUser);

    /**
     * campaign回冲更新逻辑
     *
     * @param params
     * @return R
     */
    R updateCampaignRecovery(Map<String, Object> params, SysUserEntity currentUser);
}
