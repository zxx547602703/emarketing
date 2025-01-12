package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.SignatoryInfo;
import com.wiko.emarket.service.reviewconfigmanage.SignatoryInfoService;
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
public class signatoryInfoController extends AbstractController {
    @Resource
    private SignatoryInfoService signatoryInfoService;

    @RequestMapping("/signatory/saveOrUpdate")
    public R saveOrUpdate(@RequestBody List<SignatoryInfo> list){
        String ret = signatoryInfoService.saveOrUpdate(list, this.getUserId());
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    @RequestMapping("/signatory/delete")
    public R saveOrUpdate(@RequestParam("id") String id){
        String ret = signatoryInfoService.deleteData(id);
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    @RequestMapping("/signatory/query")
    public PageUtils queryList(@RequestBody Map params){
        PageUtils object = signatoryInfoService.queryList(this.getUserId(), params);
        return object;
    }

    @RequestMapping("/signatory/exportExcel")
    public void exportCampaignBudget(@RequestParam Map<String,Object> params, HttpServletResponse response){
        try {
            signatoryInfoService.exportExcel(params, response);
        } catch (Exception e) {
            log.error("exportCampaignBudget error", e);
        }
    }
}
