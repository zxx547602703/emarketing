package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.FirstReviewerInfo;
import com.wiko.emarket.service.reviewconfigmanage.FirstReviewerConfigService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.vo.UserLikeVo;
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
 * @title: 初验人配置
 * @projectName: emarketing
 * @description: 初验人员配置
 * @date: 2022/8/8 13:53
 */
@RestController
@Slf4j
@RequestMapping("/v1")
public class FirstReviewerConfigController extends AbstractController {

    @Resource
    private FirstReviewerConfigService firstReviewerConfigService;

    /**
     * 保存按钮的接口，包括新增修改的操作
     * @param list
     * @return
     */
    @RequestMapping("/acceptance/saveFirstReviewer")
    public R saveFirstReviewer(@RequestBody List<FirstReviewerInfo> list){
        String ret = firstReviewerConfigService.saveFirstReviewer(list, this.getUserId());
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    /**
     * 删除按钮的接口
     * @param params id的集合
     * @return
     */
    @RequestMapping("/acceptance/delFirstReviewer")
    public R delFirstReviewer(@RequestBody Map<String,List<String>> params){
        String ret = firstReviewerConfigService.delFirstReviewer(params.get("ids"));
        if(ret.equals("success")){
            return R.ok(ret);
        } else{
            return R.error(ret);
        }
    }

    /**
     * 第一验收人的查询
     * @param params 查询条件
     * @return
     */
    @RequestMapping("/acceptance/queryFirstReviewer")
    public R queryFirstReviewer(@RequestBody Map<String,Object> params){
        PageUtils object = firstReviewerConfigService.queryFirstReviewer(params);
        return R.ok().put("data",object);
    }

    /**
     * 创建验收单时第一验收人的适配
     * @param areaCodes Campaign中区域code按代表处，地区部，全球的顺序
     * @param subjectCodes 科目的code
     * @return
     */
    @RequestMapping("/acceptance/getSingleFirstReviewer")
    public R getSingleFirstReviewer(@RequestParam String areaCodes, @RequestParam String subjectCodes){
        UserLikeVo singleReviewer = firstReviewerConfigService.getSingleFirstReviewer(areaCodes, subjectCodes);
        if(null !=singleReviewer){
            return R.ok().put("data",singleReviewer);
        }else{
            return R.error().put("msg", I18nUtil.getMessage("QueryFirstReviewNull"));
        }

    }

    /**
     * 导出按钮接口
     * @param params
     * @param response
     */
    @RequestMapping("/acceptance/exportFirstReviewer")
    public void exportFirstReviewer(@RequestParam Map<String,Object> params, HttpServletResponse response){
        try {
            firstReviewerConfigService.exportFirstReviewer(params, response);
        } catch (Exception e) {
            log.error("exportCampaignBudget error", e);
        }
    }


}
