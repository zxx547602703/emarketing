package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.ActivityType;
import com.wiko.emarket.service.campaign.ActivityTypeService;
import com.wiko.emarket.util.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/27 16:41
 * @description: TODO
 **/
@RestController
@RequestMapping("/api/activityType")
public class ActivityTypeController extends AbstractController {

    @Autowired
    private ActivityTypeService activityTypeService;

    /**
     * 三级分类树
     */
    @RequestMapping("/list/tree")
    public R listWithTree(@RequestBody Map<String, Object> param){
        List<ActivityType> activityTypeList = activityTypeService.listWithTree(param);
        return R.ok().put("data",activityTypeList);
    }

    /**
     * 获取采购方式
     */
    @RequestMapping("/getPurchaseType")
    public R getPurchaseType(@RequestBody Map<String, Object> param){
        return R.ok().put("dicts", activityTypeService.getPurchaseType(param));
    }

    /**
     * 业务科目
     */
    @RequestMapping("/activity_type/{level}/{typeName}")
    public R getActivityType(@PathVariable("level") String level,@PathVariable("typeName") String typeName){
        List<ActivityType> activityTypeList = activityTypeService.getActivityType(level,typeName);
        return R.ok().put("data",activityTypeList);
    }

    @RequestMapping("/updateActivityType")
    public R updateActivityType(@RequestBody ActivityType activityType) {
        activityTypeService.updateActivityType(activityType,this.getUserId());
        return R.ok().put("msg", I18nUtil.getMessage("UpdateActivityTypeSuccess"));
    }

    @RequestMapping("/deleteActivityType")
    public R deleteActivityType(@RequestBody ActivityType activityType) {
        activityTypeService.deleteActivityType(activityType);
        return R.ok().put("msg",I18nUtil.getMessage("DeleteDataSuccess"));
    }

    @RequestMapping("/insertActivityType")
    public R insertAreaTree(@RequestBody ActivityType activityType) {
        activityTypeService.insertActivityType(activityType,getUserId());
        return R.ok().put("msg",I18nUtil.getMessage("InsertDataSuccess"));
    }

    @RequestMapping("/queryActivityType")
    public R queryActivityType() {
        List<ActivityType> list = activityTypeService.queryActivityType();
        return R.ok().put("data",list);
    }

    /**
     * 业务专家配置查询业务科目树
     * @return
     */
    @RequestMapping("/bussinessExtConfig/queryActivityTypeTree")
    public R queryActivityTypeTree() {
        return activityTypeService.queryActivityTypeTree();
    }

    /**
     * 业务专家配置查询业务科目树
     * @param params
     * @return R
     */
    @RequestMapping("/queryActivityTypeByLevel")
    public R queryActivityTypeByLevel(@RequestParam Map<String,Object> params){
        return activityTypeService.queryActivityTypeByLevel(params);
    }

    /**
     * 验收人（包括初验和复合验收人）查询业务科目树
     * @return
     */
    @RequestMapping("/reviewerConfig/queryActivityTypeTree")
    public R queryActivityTypeTreeForReviewer(@RequestParam String areaCode) {
        return activityTypeService.queryActivityTypeTreeForReviewer(areaCode);
    }
}
