package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.util.IdUtil;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.shiro.ShiroUtils;
import com.wiko.emarket.constant.CampaignStatusEnum;
import com.wiko.emarket.dao.BudgetDao;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.MarketingBudgetVO;
import com.wiko.emarket.service.campaign.MarketingBudgetService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.marketingBudget.AmountVO;
import com.wiko.emarket.vo.marketingBudget.ShowBudgetVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author WIKO
 * @Date: 2022/4/27 - 04 - 27 - 15:50
 * @projectName:PSI
 * @Description: com.wiko.emarket.service
 */
@Service
public class MarketingBudgetServiceImpl implements MarketingBudgetService {
    @Autowired
    private BudgetDao budgetDao;
    /**
     * 全球level
     */
    public static final String HQ_LEVEL = "1";
    /**
     * 区域level
     */
    public static final String RG_LEVEL = "2";
    /**
     * 国家level
     */
    public static final String CTY_LEVEL = "3";
    /**
     * HQ的区域code
     */
    public static final String RG_HQ = "RG_HQ";


    @Override
    public List<MarketingBudgetVO> queryMarketingBudget(Map<String, Object> params) {
        params.put("language", RequestUtil.getLang());
        List<MarketingBudgetVO> marketingBudgetVOS = budgetDao.queryMarketingBudget(params);
        return marketingBudgetVOS;
    }

    @Override
    public Map<String, Object> deleteMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs) {
        HashMap<String, Object> map = new HashMap<>(2);
        Map<String, Object> budgetMap = new HashMap<>(2);
        budgetMap.put("list", marketingBudgetVOs);
        String lang = RequestUtil.getLang();
        budgetMap.put("language", lang);
        List<String> budgetNames = budgetDao.findSubBudget(budgetMap);
        if (!budgetNames.isEmpty()) {
            String budgetName = String.join(",", budgetNames);
            map.put("errorCode", 100);
            map.put("msg", budgetName + I18nUtil.getMessage("existSub"));
            return map;
        }
        budgetDao.deleteMarketingBudget(marketingBudgetVOs);
        map.put("errorCode", 0);
        map.put("msg", "success");
        return map;
    }

    @Override
    public Map<String, Object> saveMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs, String lang) {
        HashMap<String, Object> map = new HashMap<>(2);
        SysUserEntity user = ShiroUtils.getUserEntity();
        // 用于校验是否提交多个相同BudgetType
        if (checkRepeat(marketingBudgetVOs)) {
            map.put("errorCode", 100);
            map.put("msg", I18nUtil.getMessage("DuplicateType"));
            return map;
        }
        for (MarketingBudgetVO vo : marketingBudgetVOs) {
            // 查询地区部预算，进行累加校验
            Map<String, String> map2 = new HashMap<String, String>();
            map2.put("level", RG_LEVEL);
            map2.put("budgetType", vo.getBudgetType());
            map2.put("parentId", vo.getAreaCode());
            map2.put("year",vo.getYear());
            BigDecimal lowerBudget = budgetDao.queryLowerBudget(map2);
            if (lowerBudget != null && vo.getAmount().compareTo(lowerBudget) == -1) {
                map.put("errorCode", 600);
                map.put("msg", I18nUtil.getMessage("InsufficientBudget"));
                return map;
            }
            vo.setUuid(IdUtil.simpleUUID());
            vo.setUpdator(user.getUserId().toString());
        }

        budgetDao.updateMarketingBudget(marketingBudgetVOs);
        map.put("errorCode", 0);
        map.put("msg", "success");
        return map;
    }

    private boolean checkRepeat(List<MarketingBudgetVO> marketingBudgetVOs) {
        HashSet<String> budgetTypeSet = new HashSet<>();
        for (MarketingBudgetVO vo : marketingBudgetVOs
        ) {
            budgetTypeSet.add(vo.getBudgetType());
        }
        if (budgetTypeSet.size() != marketingBudgetVOs.size()) {
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> saveMarketingBudgetLv1(List<MarketingBudgetVO> marketingBudgetVOs, String lang) {
        HashMap<String, Object> map = new HashMap<>(2);
        Map<String, String> map1 = new HashMap<String, String>();
        map1.put("level", HQ_LEVEL);
        // 用于校验是否提交多个相同BudgetType
        if (checkRepeat(marketingBudgetVOs)) {
            map.put("errorCode", 100);
            map.put("msg", I18nUtil.getMessage("DuplicateType"));
            return map;
        }
        SysUserEntity user = ShiroUtils.getUserEntity();
        for (MarketingBudgetVO vo : marketingBudgetVOs) {
            map1.put("budgetType", vo.getBudgetType());
            map1.put("year",vo.getYear());
            // 查询全球数据，进行累加校验
            MarketingBudgetVO marketingBudgetVO = budgetDao.queryAmountUUid(map1);
            // 查询该地区部下级国家预算（level=3），进行累加校验
            Map<String, String> map2 = new HashMap<String, String>();
            map2.put("level", CTY_LEVEL);
            map2.put("budgetType", vo.getBudgetType());
            map2.put("parentId", vo.getAreaCode());
            map2.put("year",vo.getYear());
            BigDecimal lowerBudget = budgetDao.queryLowerBudget(map2);
            // 查询'其余'地区部数据，进行累加校验
            Map<String, Object> marketingBudgetMap = new HashMap<String, Object>();
            marketingBudgetMap.put("level", vo.getLevel());
            marketingBudgetMap.put("budgetType", vo.getBudgetType());
            marketingBudgetMap.put("year",vo.getYear());
            marketingBudgetMap.put("areaCode",vo.getAreaCode());
            List<MarketingBudgetVO> budgetVOS = budgetDao.queryOtherMarketingBudget(marketingBudgetMap);

            BigDecimal amount = BigDecimal.ZERO; // 所有地区部预算和
            amount = amount.add(vo.getAmount());
            for (MarketingBudgetVO vo1 : budgetVOS) {
                if (vo1.getAreaCode().equals(vo.getAreaCode())) {
                    amount = amount.add(vo1.getAmount());
                }
            }
            if (marketingBudgetVO == null || marketingBudgetVO.getAmount() == null) {
                map.put("errorCode", 400);
                map.put("msg", String.format(I18nUtil.getMessage("pleaseConfigGlobal"),vo.getBudgetTypeName()));
                return map;
            }
            // 国家预算(lowerBudget)不能超过对应的地区部预算(vo.getAmount)
            if (lowerBudget != null && vo.getAmount().compareTo(lowerBudget) == -1) {
                map.put("errorCode", 600);
                map.put("msg", vo.getBudgetTypeName() + I18nUtil.getMessage("InsufficientBudgetArea"));
                return map;
            }
            // 所有地区部某预算分类的预算和(amount) 不能超过 某预算分类的全球预算(marketingBudgetVO.getAmount())
            if (marketingBudgetVO.getAmount().compareTo(amount) == -1) {
                map.put("errorCode", 300);
                map.put("msg", vo.getBudgetTypeName() + I18nUtil.getMessage("InsufficientBudgetGlobal"));
                return map;
            }
            vo.setParentId(marketingBudgetVO.getUuid());
            vo.setUuid(IdUtil.simpleUUID());
            vo.setUpdator(user.getUserId().toString());
        }
        budgetDao.updateMarketingBudget(marketingBudgetVOs);
        map.put("errorCode", 0);
        map.put("msg", "success");
        return map;

    }


    @Override
    public List<ShowBudgetVO> queryHqBudget(String year) {
        List<ShowBudgetVO> showBudgetVOS = new ArrayList<>();
        showBudgetVOS.add(buildHqBudget(year, "MBF", null, I18nUtil.getMessage("globe"), HQ_LEVEL));
        showBudgetVOS.add(buildHqBudget(year, "MSF", null, I18nUtil.getMessage("globe"), HQ_LEVEL));
        return showBudgetVOS;
    }

    private ShowBudgetVO buildHqBudget(String year, String source, String areaCode, String areaName, String level) {
        HashMap<String, String> map = new HashMap<>();
        map.put("year", year);
        map.put("level", level);
        map.put("source", source);
        map.put("areaCode", areaCode);
        ShowBudgetVO showbudgetVO = new ShowBudgetVO();
        // 查询国家地区已收益金额
        List<AmountVO> amountVOS = budgetDao.queryAmountRate(map);
        BigDecimal benefitAmount = BigDecimal.ZERO;
        for (AmountVO vo : amountVOS) {
            if (vo.getAmount() != null && vo.getSaveLatestRate() != null) {
                if (CampaignStatusEnum.CLOSED.getStatusCode().equals(vo.getCampaignStatus())) {
                    // cmapaign已关闭， 回冲金额回预算池, 否则不回
                    benefitAmount = benefitAmount.add(MathUtils.subtract(vo.getAmount().multiply(vo.getSaveLatestRate()), vo.getRecoveryAmountUsd()));
                } else {
                    benefitAmount = benefitAmount.add(vo.getAmount().multiply(vo.getSaveLatestRate()));
                }
            }
        }
        // 查询国家地区总预算
        BigDecimal budgetAmount = budgetDao.queryHqBudget(map);
        if (budgetAmount == null) {
            budgetAmount = BigDecimal.ZERO;
            showbudgetVO.setBudgetAmount(BigDecimal.ZERO);
        } else {
            showbudgetVO.setBudgetAmount(budgetAmount);
        }

        showbudgetVO.setBenefitAmount(MathUtils.round(benefitAmount,2));
        if ("MSF".equalsIgnoreCase(source)) {
            // MSF没有 季度累计预算
            showbudgetVO.setSurplusBudget(BigDecimal.ZERO);
        } else {
            BigDecimal surplusBudgetAmount = null != benefitAmount ? budgetAmount.subtract(benefitAmount) : budgetAmount;
            showbudgetVO.setSurplusBudget(surplusBudgetAmount);
        }

        showbudgetVO.setAreaCode(areaCode);
        showbudgetVO.setAreaName(areaName);
        showbudgetVO.setYear(year);
        showbudgetVO.setSource(source);
        return showbudgetVO;
    }

    @Override
    public List<ShowBudgetVO> queryAreaBudget(Map<String, String> map) {
        String year = map.get("year");
        String areaCode = map.get("areaCode");
        String areaName = map.get("areaName");
        List<ShowBudgetVO> showBudgetVOS = new ArrayList<>();
        showBudgetVOS.add(buildHqBudget(year, "MBF", null, I18nUtil.getMessage("globe"), HQ_LEVEL));
        showBudgetVOS.add(buildHqBudget(year, "MBF", areaCode, areaName, RG_LEVEL));
        if(RG_HQ.equals(areaCode)){
            showBudgetVOS.add(buildHqBudget(year, "MSF", areaCode, areaName, RG_LEVEL));
        }
        return showBudgetVOS;
    }

    @Override
    public List<BudgetTypeEntity> queryBudgetType(Map<String, String> map) {
        map.put("language", RequestUtil.getLang());
        List<BudgetTypeEntity> budgetTypeEntities = budgetDao.queryBudgetType(map);
        if(RG_HQ.equals(map.get("areaCode"))){
            List<BudgetTypeEntity> hqBudgetTypeList = budgetTypeEntities.stream().filter(budget -> !"PM".equals(budget.getCode())).collect(Collectors.toList());
            return hqBudgetTypeList;
        }
        return budgetTypeEntities;
    }

}
