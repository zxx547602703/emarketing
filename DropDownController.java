package com.wiko.emarket.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.utils.R;
import com.wiko.emarket.constant.AreaLevelEnum;
import com.wiko.emarket.constant.CampaignStatusEnum;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.AreaDao;
import com.wiko.emarket.dao.BudgetTypeDao;
import com.wiko.emarket.dao.CountryMarketingBudgetDao;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;
import com.wiko.emarket.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * emarket下拉选相关请求
 *
 */
@RestController
@RequestMapping("/api/emarket")
@Slf4j
public class DropDownController {
    // 按照中文首字母排序
    private static Comparator COMPARTOR = Collator.getInstance(java.util.Locale.CHINA);

    @Autowired
    private AreaDao areaDao;

    @Autowired
    private CountryMarketingBudgetDao marketingBudgetDao;

    @Autowired
    private BudgetTypeDao budgetTypeDao;

    /**
     * 查询地区下拉选
     */
    @RequestMapping("/getAreas")
    public R getAreas(@RequestParam Map<String, Object> params) {
        List<AreaEntity> areaVoList = areaDao.selectList(null);
        if(CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())){
            for (AreaEntity area:areaVoList
            ) {
                area.setNameCn(area.getNameEn());
            }
        }


        // 地区(加上全球)

        List<AreaEntity> regions = areaVoList.stream().filter(vo ->  Arrays.asList(AreaLevelEnum.REGION.getLevel(), AreaLevelEnum.WORLDWIDE.getLevel()).contains(vo.getLevel())).distinct().collect(Collectors.toList());
        Collections.sort(regions , (o1, o2) -> {
            return COMPARTOR.compare(o1.getNameCn(), o2.getNameCn());
        });

        // 代表处
        List<AreaEntity> represents = areaVoList.stream().filter(vo -> StringUtils.equals(vo.getLevel(), AreaLevelEnum.REPRESENT_OFFICE.getLevel())).distinct().collect(Collectors.toList());

        Collections.sort(represents , (o1, o2) -> {
            return COMPARTOR.compare(o1.getNameCn(), o2.getNameCn());
        });

        // 国家
        List<AreaEntity> countrys = areaVoList.stream().filter(vo -> StringUtils.equals(vo.getLevel(), AreaLevelEnum.COUNTRY.getLevel())).distinct().collect(Collectors.toList());
        Collections.sort(countrys , (o1, o2) -> {
            return COMPARTOR.compare(o1.getNameCn(), o2.getNameCn());
        });

        return R.ok().put("regions", regions).put("represents", represents).put("countrys", countrys);
    }

    /**
     * 查询代表处下拉选
     */
    @RequestMapping("/getCountryByRegion")
    public R getCountryByRegion(@RequestParam Map<String, Object> params) {
        Object areaCode = params.get("region");
        List<AreaEntity> countrys = null;

        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("code", areaCode);
        List<AreaEntity> areaEntities = areaDao.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(areaEntities) && AreaLevelEnum.WORLDWIDE.getLevel().equals(areaEntities.get(0).getLevel())) {
            // 地区部选择了全球
            EntityWrapper<AreaEntity> areaWrapper2 = new EntityWrapper<>();
            areaWrapper2.eq("level", AreaLevelEnum.REPRESENT_OFFICE.getLevel());
            countrys = areaDao.selectList(areaWrapper2);
        } else if (null != areaCode && "" != areaCode) {
            EntityWrapper<AreaEntity> areaWrapper2 = new EntityWrapper<>();
            areaWrapper2.eq("parent_id", areaCode.toString());
            countrys = areaDao.selectList(areaWrapper2);
        } else {
            // 没有选择地区部
            EntityWrapper<AreaEntity> areaWrapper2 = new EntityWrapper<>();
            areaWrapper2.eq("level", AreaLevelEnum.REPRESENT_OFFICE.getLevel());
            countrys = areaDao.selectList(areaWrapper2);
        }

        // 国家排序
        Collections.sort(countrys , (o1, o2) -> {
            return COMPARTOR.compare(o1.getNameCn(), o2.getNameCn());
        });


        return R.ok().put("countrys", countrys);
    }

    /**
     * 通过代表处查询地区下拉选
     */
    @RequestMapping("/getRegionByRepresent")
    public R getRegionByRepresent(@RequestParam Map<String, Object> params) {
        Object representCode = params.get("represent");
        List<AreaEntity> areas = null;
        if (null != representCode && "" != representCode) {
            EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
            areaWrapper.eq("code", representCode.toString());
            areas = areaDao.selectList(areaWrapper);
            if (CollectionUtils.isEmpty(areas)) {
                return R.ok().put("regions", Collections.emptyList());
            }
            List<String> parentIds = areas.stream().map(AreaEntity::getParentId).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
            EntityWrapper<AreaEntity> areaWrapper1 = new EntityWrapper<>();
            areaWrapper1.in("code", parentIds);
            areas = areaDao.selectList(areaWrapper1);
        } else {
            EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
            areaWrapper.in("level", Arrays.asList(AreaLevelEnum.REGION.getLevel(),AreaLevelEnum.WORLDWIDE.getLevel()));
            areas = areaDao.selectList(areaWrapper);
        }

        // 地区排序
        Collections.sort(areas , (o1, o2) -> {
            return COMPARTOR.compare(o1.getNameCn(), o2.getNameCn());
        });

        return R.ok().put("regions", areas);
    }

    /**
     * 查询年份下拉选
     */
    @RequestMapping("/getYears")
    public R getYears(@RequestParam Map<String, Object> params) {
        List<CountryMarketingBudgetEntity> marketingBudgetVoList = marketingBudgetDao.selectList(null);
        // 年份
        List<String> years = marketingBudgetVoList.stream().map(CountryMarketingBudgetEntity::getYear)
                .filter(StringUtils::isNotEmpty).distinct().sorted().collect(Collectors.toList());
        return R.ok().put("years", years);
    }

    /**
     * 查询Campaign状态下拉选
     */
    @RequestMapping("/getCampaignStatus")
    public R getCampaignDropDown(@RequestParam Map<String, Object> params) {
        EnumSet<CampaignStatusEnum> enumSet = EnumSet.allOf(CampaignStatusEnum.class);
        List<String> campaignStatusCns = new ArrayList<>();
        // 需要排除的campaign的状态
        List<String> filterStatusCode = Arrays.asList(CampaignStatusEnum.APPROVAL_FAILED.getStatusCode(), CampaignStatusEnum.CHANGE_FAILED.getStatusCode(), CampaignStatusEnum.DELETE.getStatusCode());
        for (CampaignStatusEnum camEnum : enumSet) {
            if (!filterStatusCode.contains(camEnum.getStatusCode())){
                campaignStatusCns.add(CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang())?camEnum.getStatusNameCn():camEnum.getStatusNameEn());
            }
        }

        return R.ok().put("campaignStatus", campaignStatusCns);
    }

    /**
     * 查询Campaign预算分类下拉选
     */
    @RequestMapping("/getBudgetTypes")
    public R getBudgetTypes(@RequestParam Map<String, Object> params) {
        List<BudgetTypeEntity> budgetTypeVoList = budgetTypeDao.selectList(null);
        if(CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())){
            for (BudgetTypeEntity entity:budgetTypeVoList) {
                entity.setNameCn(entity.getNameEn());
            }
        }
        // campaign code排序
        budgetTypeVoList = budgetTypeVoList.stream().sorted(Comparator.comparing(BudgetTypeEntity::getCode)).collect(Collectors.toList());
        return R.ok().put("budgetTypes", budgetTypeVoList);
    }
}
