package com.wiko.emarket.controller;

import com.alibaba.fastjson.JSON;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.ReviewerInfo;
import com.wiko.emarket.service.reviewconfigmanage.ReviewerConfigService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.vo.UserLikeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/v1")
public class ReviewerConfigController extends AbstractController {
    @Resource
    private ReviewerConfigService reviewerConfigService;

    @RequestMapping("/acceptance/saveReviewer")
    public R saveOrUpdate(@RequestBody List<ReviewerInfo> list){
        String ret = reviewerConfigService.saveOrUpdate(list, this.getUserId());
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }


    @RequestMapping("/acceptance/delReviewer")
    public R delData(@RequestBody Map<String,List<String>> params){
        String ret = reviewerConfigService.delData(params.get("ids"));
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    @RequestMapping("/acceptance/queryReviewer")
    public R queryList(@RequestBody Map<String,Object> params){
        PageUtils object = reviewerConfigService.queryList(params);
        return R.ok().put("data",object);
    }

    @RequestMapping("/acceptance/getSingleReviewer")
    public R getSingleReviewer(@RequestParam String areaCodes, @RequestParam String subjectCodes){
        UserLikeVo singleReviewer = reviewerConfigService.getSingleReviewer(areaCodes, subjectCodes);
        if(null !=singleReviewer){
            return R.ok().put("data",singleReviewer);
        }else{
            return R.error().put("msg", I18nUtil.getMessage("QueryReviewNull"));
        }

    }


    @RequestMapping("/acceptance/exportExcel")
    public void exportCampaignBudget(@RequestParam Map<String,Object> params, HttpServletResponse response){
        try {
            reviewerConfigService.exportExcel(params, response);
        } catch (Exception e) {
            log.error("exportCampaignBudget error", e);
        }
    }

    @RequestMapping("/acceptance/queryPermission")
    public R queryPermission(){
        try {
            Map<String, Boolean> permission = reviewerConfigService.getPermission(this.getUserId());
            return R.ok().put("data",permission);
        } catch (Exception e) {
            log.error("get user permission error");
            log.error(JSON.toJSONString(e));
            return R.error().put("msg","get user permission error");
        }


    }

}
