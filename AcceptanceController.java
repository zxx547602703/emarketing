package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.emarket.service.acceptance.AcceptanceService;
import com.wiko.emarket.vo.UserLikeVo;
import com.wiko.emarket.vo.po.AcceptanceCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Author shaofeng Guo
 * @Date 2022/6/30 15:54
 * @description: TODO
 **/
@RestController
@RequestMapping("/api/acceptance")
@Slf4j
public class AcceptanceController extends AbstractController {
    @Autowired
    private SysUserService userService;

    @Autowired
    private AcceptanceService acceptanceService;

    /**
     * 查询第一验收人
     *
     * @return
     */
    @RequestMapping("/queryAccept")
    public R queryAccept(@RequestParam String roleName){
        List<UserLikeVo> list =  userService.selectLike(roleName);
        return R.ok().put("list",list);
    }

    /**
     *  查询campaign信息
     *
     * @param params
     * @return
     */
    @RequestMapping("/queryCampaignInfo")
    public R queryCampaignInfo(@RequestBody Map<String,Object> params){
        return acceptanceService.queryCampaignInfo(params);
    }

    /**
     *  查询PoLine详情
     *
     * @param params
     * @return
     */
    @RequestMapping("/acceptanceDetails")
    public R acceptanceDetails(@RequestBody Map<String,Object> params){
        return acceptanceService.acceptanceDetails(params);
    }

    /**
     *  验收单保存
     *
     * @param acceptanceCreateVO
     * @return
     */
    @RequestMapping("/save")
    public R save(@RequestBody AcceptanceCreateVO acceptanceCreateVO){
        Long userId = this.getUser().getUserId();
        return acceptanceService.create(acceptanceCreateVO, userId);
    }

    /**
     *  验收单提交
     *
     * @param acceptanceCreateVO
     * @return
     */
    @RequestMapping("/submit")
    public R submit(@RequestBody AcceptanceCreateVO acceptanceCreateVO){
        Long userId = this.getUser().getUserId();
        return acceptanceService.submit(acceptanceCreateVO, userId);
    }

    /**
     *  验收单列表
     *
     * @param params
     * @return
     */
    @RequestMapping("/list")
    public R list(@RequestBody Map<String,Object> params){
        PageUtils page;
        try {
            Long userId = this.getUser().getUserId();
            page = acceptanceService.list(params,userId);
        } catch (Exception e) {
            log.error("getAcceptanceListInfo error", e);
            return R.error("查询Acceptance列表失败");
        }
        return R.ok().put("page",page);
    }

    /**
     *  获取复核人
     *
     * @param params
     * @return
     */
    @RequestMapping("/getReviewer")
    public R getReviewer(@RequestBody Map<String,Object> params){
        String campaignId = (String) params.get("campaignId");
        return acceptanceService.getReviewerApi(campaignId);
    }

    /**
     *  导出验收单列表信息
     *
     * @param params
     * @param response
     */
    @GetMapping("/export")
    public void exportAcceptanceExcel(@RequestParam Map<String,Object> params, HttpServletResponse response) {
        try {
            Long userId = this.getUser().getUserId();
            acceptanceService.exportAcceptanceExcel(params,userId, response);
        } catch (Exception e) {
            log.error("exportAcceptanceExcel error", e);
        }
    }

    /**
     *  活动执行人角色
     *
     * @return
     */
    @RequestMapping("/isActivityExecutor")
    private R isActivityExecutor(){
        try {
            Long userId = this.getUser().getUserId();
            return acceptanceService.isActivityExecutor(userId);
        } catch (Exception e) {
            log.error("isActivityExecutor error", e);
            return R.error();
        }
    }

    /**
     *  业务主管角色
     *
     * @return
     */
    @RequestMapping("/isSupervisor")
    private R isSupervisor(){
        try {
            Long userId = this.getUser().getUserId();
            return acceptanceService.isSupervisor(userId);
        } catch (Exception e) {
            log.error("isSupervisor error", e);
        }
        return R.ok();
    }

    /**
     *  复核验收角色
     *
     * @return
     */
    @RequestMapping("/isReviewer")
    private R isReviewer(){
        try {
            Long userId = this.getUser().getUserId();
            return acceptanceService.isReviewer(userId);
        } catch (Exception e) {
            log.error("isReviewer error", e);
        }
        return R.ok();
    }

    /**
     * 查询待办转移人员列表
     *
     * @return
     */
    @RequestMapping("/getTransferUsers")
    private R getTransferUsers(@RequestBody Map<String, Object> params){
        try {
            return R.ok().put("data", acceptanceService.getTransferUsers(params));
        } catch (Exception e) {
            log.error("getTransferUsers error", e);
        }
        return R.ok();
    }
}

