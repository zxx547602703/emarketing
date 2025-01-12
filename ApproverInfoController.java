package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.ApproverInfo;
import com.wiko.emarket.service.reviewconfigmanage.ApproverInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/6/7 9:04
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class ApproverInfoController extends AbstractController {
    @Resource
    private ApproverInfoService approverInfoService;

    @RequestMapping("/approver/saveOrUpdate")
    public R saveOrUpdate(@RequestBody List<ApproverInfo> list){
        String ret = approverInfoService.saveOrUpdate(list, this.getUserId());
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    @RequestMapping("/approver/delete")
    public R saveOrUpdate(@RequestParam("id") String id){
        String ret = approverInfoService.deleteData(id);
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    @RequestMapping("/approver/query")
    public PageUtils queryList(@RequestBody Map<String,Object> params){
        PageUtils object = approverInfoService.queryList(this.getUserId(),params);
        return object;
    }

    @RequestMapping("/approver/exportExcel")
    public void exportCampaignBudget(@RequestParam Map<String,Object> params, HttpServletResponse response){
        try {
            approverInfoService.exportExcel(params, response);
        } catch (Exception e) {
            log.error("exportCampaignBudget error", e);
        }
    }

}
