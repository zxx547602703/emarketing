package com.wiko.emarket.controller;

import com.framework.common.annotation.SysLog;
import com.framework.common.utils.R;
import com.wiko.emarket.constant.ErrorCode;
import com.wiko.emarket.constant.StatusCodeEnum;
import com.wiko.emarket.service.campaign.CampaignAcceptanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @ClassName CampaignAcceptanceController
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/8/5 9:19
 * @Version 1.0
 **/
@RestController
@Slf4j
@RequestMapping("/api/acceptance")
public class CampaignAcceptanceController {
    private final String regx = "^[0-9]*[1-9][0-9]*$";
    @Autowired
    private CampaignAcceptanceService campaignAcceptanceService;
    // 我的待办标识符
    private final String MY="my";
    // 创建待办标识符
    private final String CREATED="created";

    /**
     * 创建验收单列表
     *
     * @param params Map<String,Object>
     * @return R
     */
    @RequestMapping("creatAcceptanceList")
    public R creatAcceptanceList(@RequestBody Map<String, Object> params) {
        log.info("creatAcceptanceList params:{}", params);
         if(MY.equals(params.get("type"))){
             // 创建人、Campaign Owner、Campaign PA、Supervisor、Reviewer、Activity Creator、流程Transfer To是当前用户
             return campaignAcceptanceService.userAcceptanceList(params);
         }

        if(CREATED.equals(params.get("type"))){
            // Campaign的提单人组织在当前用户的授权区域内，并且与我相关的则显示
            return campaignAcceptanceService.creatAcceptanceList(params);
        }

        // campaign详情页的PR详情,  目前该分支不会走了已改成了campaign执行信息tab页
        return campaignAcceptanceService.queryAcceptanceByCampaignId(params);
    }

    /**
     * 导出创建验收单
     *
     * @param params   Map<String,Object>
     * @param response
     */
    @RequestMapping("exportCreatAcceptance")
    public void exportCreatAcceptance(@RequestParam Map<String, Object> params, HttpServletResponse response) throws IOException {
        log.info("exportCreatAcceptance params ={}"+params);
        if(CREATED.equals(params.get("type"))){
            // campaign验收_查看验收单列表导出
            campaignAcceptanceService.exportCreatAcceptance(params, response);
        }else if (MY.equals(params.get("type"))) {
            // 与我相关_我的验收列表导出
            campaignAcceptanceService.exportUserAcceptance(params, response);
        } else {
            // campaign 详情——PR详情列表导出
            campaignAcceptanceService.exportDetailAcceptance(params, response);
        }
    }

    /**
     * 根据用户账号模糊查询执行人
     *
     * @param params Map<String,Object>
     * @return R
     */
    @RequestMapping("queryUserCordByUserName")
    public R queryUserCordByUserName(@RequestParam Map<String, Object> params) {
        return campaignAcceptanceService.queryUserCordByUserName(params);
    }


    /**
     * 查看验收单列表
     *
     * @param params Map<String,Object>
     * @return R
     */
    @RequestMapping("viewAcceptanceList")
    public R viewAcceptanceList(@RequestBody Map<String, Object> params) {
        log.info("viewAcceptanceList params ={}"+params);
        if (params.get("currentPage") == null || params.get("pageSize") == null) {
            return R.error(Integer.parseInt(ErrorCode.PARAM_ERROR), "params error");
        }
        return campaignAcceptanceService.viewAcceptanceList(params);
    }

    /**
     * 导出验收单列表
     *
     * @param params   Map<String,Object>
     * @param response
     */
    @RequestMapping("exportViewAcceptanceList")
    public void exportViewAcceptanceList(@RequestParam Map<String, Object> params, HttpServletResponse response) throws IOException {
        log.info("exportViewAcceptanceList params ={}"+params);
        campaignAcceptanceService.viewAcceptanceExport(params, response);
    }

    /**
     * 验收状态映射关系
     *
     * @return R
     */
    @RequestMapping("queryAcceptanceFormStatus")
    public R queryAcceptanceFormStatus(){
        return campaignAcceptanceService.queryAcceptanceFormStatus();
    }

    /**
     * 校验验收人
     *
     * @param params
     * @return R
     */
    @RequestMapping("checkAcceptor")
    public R checkAcceptor(@RequestBody Map<String,Object> params) {
        return  campaignAcceptanceService.checkAcceptor(params);
    }

    /**
     * 校验验收人
     *
     * @param params
     * @return R
     */
    @SysLog("删除验收单")
    @RequestMapping("deleteAcceptance")
    public R deleteAcceptance(@RequestParam Map<String,Object> params){
        log.warn("view acceptance deleteAcceptance params ={}"+params);
        if(params.get("id")==null){
            R.error(StatusCodeEnum.PARAM_ERROR.getStatusCode(),"params is null");
        }
        return campaignAcceptanceService.deleteAcceptance(Long.parseLong(params.get("id").toString()) );
    }
}
