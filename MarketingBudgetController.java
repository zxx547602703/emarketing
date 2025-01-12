package com.wiko.emarket.controller;


import com.framework.common.annotation.SysLog;
import com.framework.common.validator.ValidatorUtils;
import com.framework.common.validator.group.AddGroup;
import com.framework.common.validator.group.UpdateGroup;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.CampaignDao;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.MarketingBudgetVO;
import com.wiko.emarket.service.campaign.CampaignService;
import com.wiko.emarket.service.campaign.MarketingBudgetService;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.marketingBudget.ShowBudgetVO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author WIKO
 * @Date: 2022/4/27 - 04 - 27 - 18:08
 * @projectName:emarket
 * @Description: com.wiko.emarket.controller
 */
@RestController
@RequestMapping("/emarket/budget")
public class MarketingBudgetController {
    @Autowired
    private MarketingBudgetService marketingBudgetService;
    /**
     * 预算年份
     */
    public static final String YEAR = "year";
    /**
     * 预算层级
     */
    public static final String LEVEL = "level";
    /**
     * 来源：MSF、MBF
     */
    public static final String SOURCE = "source";
    private final Logger logger = LoggerFactory.getLogger(MarketingBudgetController.class);
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private CampaignDao campaignDao;

    /**
     * 查询预算
     */
    @SysLog("查询营销预算信息")
    @RequestMapping("/queryMarketingBudget")
    @RequiresPermissions("emarket:budget:queryMarketingBudget")
    public List<MarketingBudgetVO> queryMarketingBudget(@RequestParam Map<String, Object> params) throws ExecutionException, InterruptedException {
        if (params.get(YEAR) == null || params.get(LEVEL) == null || params.get(SOURCE) == null) {
            return null;
        }
        List<MarketingBudgetVO> marketingBudgetVOS = marketingBudgetService.queryMarketingBudget(params);
        return marketingBudgetVOS;
    }

    /**
     * 修改用户
     */
    @SysLog("失效营销预算数据")
    @RequestMapping("/deleteMarketingBudget")
    @RequiresPermissions("emarket:budget:deleteMarketingBudget")
    public Map<String, Object> deleteMarketingBudget(@RequestBody List<MarketingBudgetVO> marketingBudgetVOs) {
        ValidatorUtils.validateEntity(marketingBudgetVOs, UpdateGroup.class);
        return marketingBudgetService.deleteMarketingBudget(marketingBudgetVOs);
    }

    /**
     * 修改用户
     */
    @SysLog("保存营销预算全球数据")
    @RequestMapping("/saveMarketingBudget")
    @RequiresPermissions("emarket:budget:saveMarketingBudget")
    public Map<String, Object> saveMarketingBudget(@RequestBody List<MarketingBudgetVO> marketingBudgetVOs) {
        ValidatorUtils.validateEntity(marketingBudgetVOs, AddGroup.class);
        //参数校验
        String lang = RequestUtil.getLang();

        Map<String, Object> map1 = checkParam(marketingBudgetVOs, lang);
        if (map1 != null) {
            return map1;
        }
        return marketingBudgetService.saveMarketingBudget(marketingBudgetVOs, lang);
    }

    /**
     * 修改用户
     */
    @SysLog("保存营销预算地区部数据")
    @RequestMapping("/saveMarketingBudgetLv1")
    @RequiresPermissions("emarket:budget:saveMarketingBudgetLv1")
    public Map<String, Object> saveMarketingBudgetLv1(@RequestBody List<MarketingBudgetVO> marketingBudgetVOs) {
        ValidatorUtils.validateEntity(marketingBudgetVOs, AddGroup.class);
        String lang = RequestUtil.getLang();
        Map<String, Object> map1 = checkParam(marketingBudgetVOs, lang);
        if (map1 != null) {
            return map1;
        }
        return marketingBudgetService.saveMarketingBudgetLv1(marketingBudgetVOs, lang);
    }

    private Map<String, Object> checkParam(List<MarketingBudgetVO> marketingBudgetVOs, String lang) {
        Map<String, Object> map = new HashMap<>();
        if (marketingBudgetVOs.size() != marketingBudgetVOs.size()) {
            map.put("errorCode", 400);
            map.put("msg", lang.equals(CommonConstant.ZH_LANGUAGE) ? "数据不能为空" : "data cannot be empty");
            return map;
        }
        for (MarketingBudgetVO vo : marketingBudgetVOs) {

            if (vo.getYear() == null || vo.getLevel() == null || vo.getAreaCode() == null
                    || vo.getBudgetType() == null || vo.getSource() == null
                    || vo.getAmount() == null || vo.getStatus() == null) {
                map.put("errorCode", 400);
                map.put("msg", lang.equals(CommonConstant.ZH_LANGUAGE) ? "数据不能为空" : "data cannot be empty");
                logger.error("saveMarketingBudget param error exist null", vo);
                return map;
            }
        }
        return null;
    }

    /**
     * 修改用户
     */
    @SysLog("查询全球预算与已启动收益金额")
    @RequestMapping("/queryHqBudget")
    @RequiresPermissions("emarket:budget:queryHqBudget")
    public List<ShowBudgetVO> queryHqBudget(@RequestParam Map<String, String> map) {
        return marketingBudgetService.queryHqBudget(map.get("year"));
    }

    /**
     * 修改用户
     */
    @SysLog("查询地区预算与已启动收益金额")
    @RequestMapping("/queryAreaBudget")
    @RequiresPermissions("emarket:budget:queryAreaBudget")
    public List<ShowBudgetVO> queryAreaBudget(@RequestParam Map<String, String> map) {
        return marketingBudgetService.queryAreaBudget(map);
    }

    /**
     * 修改用户
     */
    @SysLog("查询预算类型")
    @RequestMapping("/queryBudgetType")
    public List<BudgetTypeEntity> queryBudgetType(@RequestParam Map<String, String> map) {
        List<BudgetTypeEntity> budgetTypes = marketingBudgetService.queryBudgetType(map);
        return budgetTypes;
    }

}
