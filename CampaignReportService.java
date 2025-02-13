package com.wiko.emarket.service.campaign;

import com.framework.common.utils.PageUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;


public interface CampaignReportService {

    /**
     * Campaign预算明细
     *
     * @param params 入参
     * @return 明细
     */
    PageUtils getCampaignBudgetList(Map<String,Object> params, Long userId);

    /**
     * 导出Campaign预算明细
     *
     * @param params Map<String,Object>
     * @param response HttpServletResponse
     */
    void exportCampaignBudget(Map<String,Object> params, Long userId, HttpServletResponse response) throws IOException;


    /**
     * 查询Campaign执行明细
     *
     * @param params 入参
     * @return 明细
     */
    PageUtils getCampaignExecuteList(Map<String,Object> params, Long userId);

    /**
     * 导出Campaign执行明细
     *
     * @param params Map<String,Object>
     * @param response HttpServletResponse
     */
    void exportCampaignExecute(Map<String,Object> params, Long userId, HttpServletResponse response) throws IOException;
}
