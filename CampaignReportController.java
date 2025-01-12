package com.wiko.emarket.controller;


import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.service.campaign.CampaignNewReportService;
import com.wiko.emarket.service.campaign.CampaignReportService;
import com.wiko.emarket.service.emarketprocess.impl.CampaignCommonService;
import com.wiko.emarket.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Campaign 报表
 */
@RestController
@RequestMapping("/api/report")
@Slf4j
public class CampaignReportController extends AbstractController {
    @Autowired
    private CampaignReportService campaignReportService;

    @Autowired
    private CampaignNewReportService campaignNewReportService;

    @Autowired
    private CampaignCommonService campaignCommonService;

    /**
     * 查询Campaign预算明细
     *
     * @param params 查询入参
     * @return R
     */
    @RequestMapping("/getCampaignBudgetDetails")
    public R getCampaginBufgetDetails(@RequestBody Map<String,Object> params) {
        PageUtils pageUtils;
        try {
            Long userId = this.getUser().getUserId();
            pageUtils = campaignReportService.getCampaignBudgetList(params, userId);
        } catch (Exception e) {
            log.error("getCampaginBufgetDetails error", e);
            return R.error("查询Campaign预算明细失败");
        }
        return R.ok().put("page", pageUtils);
    }


    /**
     * 导出Campaign预算明细
     *
     * @param response HttpServletResponse
     * @param params Map
     */
    @GetMapping("/campaignBudget/export")
    public void exportCampaignBudget(@RequestParam Map<String,Object> params, HttpServletResponse response){
        try {
            Long userId = this.getUser().getUserId();
            // Financial Administrator 只有财经管理员才有权限查看
            boolean isFinancialRole = campaignCommonService.checkSysUserRole(userId, Arrays.asList("Financial administrator"));
            if (!isFinancialRole) {
                return;
            }
            campaignReportService.exportCampaignBudget(params,userId, response);
        } catch (Exception e) {
            log.error("exportCampaignBudget error", e);
        }
    }


    /**
     * 查询Campaign执行明细
     *
     * @param params 查询入参
     * @return R
     */
    @RequestMapping("/getCampaignExecuteDetails")
    public R getCampaginExecuteDetails(@RequestBody Map<String,Object> params) {
        PageUtils pageUtils;
        try {
            Long userId = this.getUser().getUserId();
            // Financial Administrator 只有财经管理员才有权限查看
            boolean isFinancialRole = campaignCommonService.checkSysUserRole(userId, Arrays.asList("Financial administrator"));
            if (!isFinancialRole) {
                return R.error(I18nUtil.getMessage("NotFinancialRole"));
            }
            pageUtils = campaignNewReportService.getCampaignExecuteList(params, userId);
        } catch (Exception e) {
            log.error("getCampaginExecuteDetails error", e);
            return R.error("查询Campaign执行明细失败");
        }
        return R.ok().put("page", pageUtils);
    }


    /**
     * 导出Campaign执行明细查询
     *
     * @param response HttpServletResponse
     * @param params Map
     */
    @RequestMapping("/campaignExecute/export")
    public void exportCampaignExecute(@RequestParam Map<String,Object> params, HttpServletResponse response) {
        Long userId = this.getUser().getUserId();
        try {
            campaignReportService.exportCampaignExecute(params, userId, response);
        } catch (IOException e) {
            log.error("exportCampaignExecute error", e);
        }
    }

    /**
     * 导出Campaign预算明细--新版本
     *
     * @param response HttpServletResponse
     * @param params Map
     */
    @RequestMapping("/campaignExecute/export/new")
    public void exportNewCampaignExecute(@RequestParam Map<String,Object> params, HttpServletResponse response) {
        Long userId = this.getUser().getUserId();
        try {
            campaignNewReportService.exportCampaignExecute(params, userId, response);
        } catch (IOException e) {
            log.error("exportCampaignExecute error", e);
        }
    }
}
