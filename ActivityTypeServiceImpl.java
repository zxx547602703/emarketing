package com.wiko.emarket.service.campaign.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.common.exception.RRException;
import com.framework.common.utils.R;
import com.framework.modules.sys.dao.SysDictDao;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.service.SysDictService;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.ActivityTypeDao;
import com.wiko.emarket.entity.ActivityType;
import com.wiko.emarket.entity.SelfPurchaseSubjectLimitVo;
import com.wiko.emarket.service.campaign.ActivityTypeService;
import com.wiko.emarket.service.campaign.CampaignService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/27 16:45
 * @description: TODO
 **/
@Service("ActivityTypeService")
@Slf4j
public class ActivityTypeServiceImpl extends ServiceImpl<ActivityTypeDao, ActivityType> implements ActivityTypeService {

    public static final String ACTIVITY_FIRST_LEVEL = "1";
    public static final String ACTIVITY_SECOND_LEVEL = "2";
    public static final String ACTIVITY_THIRD_LEVEL = "3";
    public static final String MARKETING_CODE = "MKT1";
    public static final String RETAIL_CODE = "MKT2";
    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SysDictDao sysDictDao;

    @Autowired
    private SysDictService sysDictService;
    /**
     * 查询树形列表
     * @return
     */
    @Override
    public List<ActivityType> listWithTree(Map<String, Object> param) {
        String budgetCode = (String) param.get("budgetType");
        String purchaseType = (String) param.get("purchaseType");
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        // 0. 根据budgetCode查询关联的活动类型的父id
        List<String> activitys =  campaignService.getActivityPidByBudgetCode(budgetCode);
        Wrapper<ActivityType> wrapper = new EntityWrapper<>();
        wrapper.in("code",activitys);
        // 1. 查询所有科目
        List<ActivityType> activityPids = baseMapper.selectList(wrapper);

        // 国际化处理
        List<ActivityType> activityTypes = baseMapper.selectList(null);
        if(!isChinese){
            activityPids.stream().forEach(a -> {
                a.setNameCn(a.getNameEn());
            });
            activityTypes.stream().forEach(a -> {
                a.setNameCn(a.getNameEn());
            });
        }

        // 2. 组装成树形结构
        List<ActivityType> level1Menus = activityPids.stream()
                // 2.1 找到所有的一级科目(特点: getParentId == 0)
                .filter(activityType -> activityType.getLevel().equals("1"))
                // 2.2 查找所有一级科目的子科目
                .map((menu) -> {
                    menu.setChildren(getSubActivityTypes(menu, activityTypes));
                    return menu;
                    // 2.3 组成List数组
                }).collect(Collectors.toList());

        // 3.过滤自行采购限制的科目，返回树形菜单
        return filterMenus(purchaseType, level1Menus);
    }

    @Override
    public List<SysDictEntity> getPurchaseType(Map<String, Object> param) {
        // 查询配置的采购方式
        List<SysDictEntity> purchaseTypes = sysDictService.findByType("purchaseType");

        String lv2Subject = (String) param.get("lv2Subject");
        String lv3Subject = (String) param.get("lv3Subject");
        if (StringUtils.isEmpty(lv2Subject)) {
            return purchaseTypes;
        }
        if (StringUtils.isEmpty(lv3Subject)) {
            return purchaseTypes.stream().filter(item -> !StringUtils.equals(item.getValue(),
                    CommonConstant.PURCHASE_TYPE_SELF_CN)).collect(Collectors.toList());
        }

        // 获取配置的自行采购， 限制范围内的科目
        List<SelfPurchaseSubjectLimitVo> selfPurchaseSubjectLimitVos = sysDictDao.selfPurchaseSubjectLimitInfo();
        if (CollectionUtils.isEmpty(selfPurchaseSubjectLimitVos)) {
            return purchaseTypes;
        }
        List<String> limitCodes =
                selfPurchaseSubjectLimitVos.stream().map(SelfPurchaseSubjectLimitVo::getCode).distinct().collect(Collectors.toList());

        if (CollectionUtils.isEmpty(limitCodes)) {
            return purchaseTypes;
        }
        if (limitCodes.contains(lv2Subject) || limitCodes.contains(lv3Subject)) {
            return purchaseTypes;
        }
        // 排除自行采购
        return purchaseTypes.stream().filter(item -> !StringUtils.equals(item.getValue(),
                CommonConstant.PURCHASE_TYPE_SELF_CN)).collect(Collectors.toList());
    }

    /**
     * 自行采购有科目选择限制， 需要进一步过滤
     *
     * @param purchaseTypeCode
     * @param level1Menus
     * @return
     */
    private List<ActivityType> filterMenus(String purchaseTypeCode, List<ActivityType> level1Menus) {
        if (StringUtils.isEmpty(purchaseTypeCode) || !CommonConstant.PURCHASE_TYPE_SELF_CN.equals(purchaseTypeCode)) {
            return level1Menus;
        }

        // 查询自行采购时的限制的LV2科目
        List<SelfPurchaseSubjectLimitVo> selfPurchaseSubjectLimitVos = sysDictDao.selfPurchaseSubjectLimitInfo();
        if (CollectionUtils.isEmpty(selfPurchaseSubjectLimitVos)) {
            return level1Menus;
        }
        List<String> limitCodes =
                selfPurchaseSubjectLimitVos.stream().map(SelfPurchaseSubjectLimitVo::getCode).distinct().collect(Collectors.toList());

        // 过滤自行采购限制的LV2科目
        for (ActivityType item : level1Menus) {
            // 过滤lv2
            List<ActivityType> filterLv2s =
                    item.getChildren().stream().filter(lv2 -> limitCodes.contains(lv2.getCode())).collect(Collectors.toList());
            item.setChildren(filterLv2s);

            // 过滤lv3
            filterLv2s.forEach(lv2 -> {
                List<ActivityType> filterLv3s =
                        lv2.getChildren().stream().filter(lv3 -> limitCodes.contains(lv3.getCode())).collect(Collectors.toList());
                lv2.setChildren(filterLv3s);
            });
        }
        return level1Menus;
    }

    @Override
    public List<ActivityType> getActivityType(String level, String typeName) {
        Wrapper<ActivityType> wrapper = new EntityWrapper<>();
        wrapper.in("level",level);
        wrapper.in("status","1");
        List<ActivityType> list = baseMapper.selectList(wrapper);
        ActivityType type = new ActivityType();
        type.setCode("MKT-1");
        type.setNameCn("ALL");
        type.setNameEn("ALL");
        list.add(type);
        List<ActivityType> list2 = new ArrayList<>();
        if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
            for(ActivityType type1: list){
                type1.setNameCn(type1.getNameEn());
            }
        }
        if(StringUtils.isNotBlank(typeName)&& !typeName.equals("null")){
            for (ActivityType temp : list) {
                if(temp.getNameCn().toUpperCase(Locale.ROOT).contains(typeName.toUpperCase())){
                    list2.add(temp);
                }
            }
            return list2;
        }
        return list;
    }

    @Override
    public String updateActivityType(ActivityType activityType, Long userId) {
        log.info("修改科目code为："+activityType.getCode());
        try {
            activityType.setUpdatedBy(userId.toString());
            activityType.setUpdatedDate(new Date());
            this.updateById(activityType);
        } catch (Exception e) {
            log.error("修改活动科目失败");
            log.error(JSON.toJSONString(e));
            throw new RRException(I18nUtil.getMessage("UpdateActivityTypeError"));
        }
        return "success";
    }

    @Override
    public String deleteActivityType(ActivityType activityType) {
        log.info("删除数据code"+activityType.getLevel()+"level为"+ activityType.getLevel());
        List<ActivityType> list = new ArrayList<>();
        list.add(activityType);
        try {
            if (activityType.getLevel().equals("3")) {

            } else if(activityType.getLevel().equals("2")){
                Wrapper<ActivityType> wrapper = new EntityWrapper<>();
                wrapper.eq("parent_id", activityType.getCode());
                List<ActivityType> activityTypes = this.selectList(wrapper);
                list.addAll(activityTypes);
            } else {
                Wrapper<ActivityType> wrapper = new EntityWrapper<>();
                wrapper.eq("parent_id", activityType.getCode());
                List<ActivityType> activityTypes = this.selectList(wrapper);
                list.addAll(activityTypes);
                Set<String> set = new HashSet<>(); //level=2code集合
                for(ActivityType type: activityTypes){
                    set.add(type.getCode());
                }
                Wrapper<ActivityType> wrapperLevellv3 = new EntityWrapper<>();
                wrapperLevellv3.in("parent_id", set);
                List<ActivityType> activityTypesLevellv3 = this.selectList(wrapper);
                list.addAll(activityTypesLevellv3);
            }
            for(ActivityType type:list){
                type.setStatus("0");
//                this.update(type,null);
            }
            this.updateBatchById(list);
        } catch (Exception e) {
            log.error("删除数据错误");
            log.error(JSON.toJSONString(e));
            throw new RRException(I18nUtil.getMessage("DeleteDataError"));
        }
        return "success";
    }

    @Override
    public String insertActivityType(ActivityType activityType, Long userId) {
        log.info("新增业务科目名称：" + activityType.getNameCn());
        try {
            Wrapper<ActivityType> wrapper = new EntityWrapper<>();
            wrapper.orderBy("code",false);
            List<ActivityType> list = this.selectList(wrapper);
            int max =0;
            for(ActivityType type:list){
                if(max < Integer.valueOf(type.getCode().substring(3))){
                    max = Integer.valueOf(type.getCode().substring(3));
                }
            }
            activityType.setCode("MKT"+(max+1));
            activityType.setStatus("1");
            activityType.setCreatedBy(userId.toString());
            activityType.setCreatedDate(new Date());
            activityType.setUpdatedBy(userId.toString());
            activityType.setUpdatedDate(new Date());
            this.insert(activityType);
        } catch (Exception e) {
            log.error("新增数据失败");
            log.error("失败原因："+JSON.toJSONString(e));
            throw new RRException(I18nUtil.getMessage("InsertDataError"));
        }
        return "success";
    }

    @Override
    public List<ActivityType> queryActivityType() {
        Wrapper<ActivityType> wrapper = new EntityWrapper<>();
        wrapper.eq("status","1");
        List<ActivityType> activityTypes = this.selectList(wrapper);
        List<ActivityType> retList = new ArrayList<>();
        //处理国际化
        for(ActivityType type:activityTypes){
            if(RequestUtil.getLang().equals(CommonConstant.EN_LANGUAGE) || RequestUtil.getLang().equals(CommonConstant.EN_US_LANGUAGE) ){
                type.setNameCn(type.getNameEn());
            }
        }
        for(ActivityType type:activityTypes){
            if(type.getLevel().equals("1")){
                String code = type.getCode();
                List<ActivityType> child = new ArrayList<>();
                type.setChildren(child);
                for(ActivityType lv2Type : activityTypes){
                    if(null != lv2Type.getParentId() && lv2Type.getParentId().equals(code)){
                        child.add(lv2Type);
                        this.getChild(activityTypes,lv2Type);
                    }
                }
                retList.add(type);
            }
        }

        return retList;
    }


    public void getChild(List<ActivityType> activityTypes,ActivityType activityType){
        List<ActivityType> child = new ArrayList<>();
        for(ActivityType type : activityTypes){
            if(null != type.getParentId() && type.getParentId().equals(activityType.getCode())) {
                child.add(type);
                activityType.setChildren(child);
            }
        }
    }

    /**
     * 递归查找所有科目的子科目
     * 从activityTypes列表中获取当前menu的子科目
     * @param menu
     * @param activityTypes
     * @return
     */
    private List<ActivityType> getSubActivityTypes(ActivityType menu, List<ActivityType> activityTypes) {
        List<ActivityType> children = activityTypes.stream()
                .filter((activityType) -> StringUtils.equals(activityType.getParentId(), menu.getCode()))
                .map(activityType -> {
                    activityType.setChildren(getSubActivityTypes(activityType, activityTypes));
                    return activityType;
                }).collect(Collectors.toList());
        return children;
    }

    /**
     * 业务专家配置时查询业务科目树
     * @return
     */
    @Override
    public R queryActivityTypeTree() {
        try {
            List<ActivityType> list = new ArrayList<>();
            ActivityType top = new ActivityType();
            list.add(top);
            top.setCode("MKT0");
            top.setNameCn("ALL");
            List<ActivityType> lv2Child = new ArrayList<>();
            top.setChildren(lv2Child);
            ActivityType mktAll = new ActivityType();
            lv2Child.add(mktAll);
            mktAll.setCode("MKT-1");
            mktAll.setNameCn("Marketing-ALL");
            Wrapper<ActivityType> wrapper = new EntityWrapper<>();
            wrapper.eq("status", "1");
            wrapper.eq("parent_id", "MKT1");
            List<ActivityType> activityTypes = this.selectList(wrapper);
            mktAll.setChildren(activityTypes);
            ActivityType retailAll = new ActivityType();
            lv2Child.add(retailAll);
            retailAll.setCode("MKT-2");
            retailAll.setNameCn("Retail-ALL");
            Wrapper<ActivityType> wrapper1 = new EntityWrapper<>();
            wrapper1.eq("status", "1");
            wrapper1.eq("parent_id", "MKT2");
            List<ActivityType> activityTypes2 = this.selectList(wrapper1);
            retailAll.setChildren(activityTypes2);
            return R.ok().put("data",list);
        } catch (Exception e) {
            log.error("查询科目信息出错");
            log.error(JSON.toJSONString(e));
            throw new RRException("QueryActivityTypeError");
        }
    }

    @Override
    public R queryActivityTypeByLevel(Map<String, Object> params) {
        Wrapper<ActivityType> wrapper = new EntityWrapper<>();
        wrapper.eq("status", "1");
        if (params.get("level") != null) {
            wrapper.eq("level", params.get("level"));
        }
        if (params.get("parentId") != null) {
            wrapper.eq("parent_id", params.get("parentId"));
        }
        List<ActivityType> activityTypes = this.selectList(wrapper);
        if(CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())){
            for (ActivityType activityType : activityTypes) {
                activityType.setNameCn(activityType.getNameEn());
            }
        }
        return R.ok().put("data", activityTypes);
    }

    /**
     * 业务专家配置时查询业务科目树
     * @return
     */
    @Override
    public R queryActivityTypeTreeForReviewer(String areaCode) {
        Wrapper<ActivityType> wrapper = new EntityWrapper<>();
        wrapper.eq("status", "1");
        List<ActivityType> allTypes = this.selectList(wrapper);
        List<ActivityType> list = new ArrayList<>();
        // 树的顶点（All）
        ActivityType top = new ActivityType();
        list.add(top);
        top.setCode("MKT0");
        top.setNameCn("ALL");
        List<ActivityType> allChild = new ArrayList<>();
        top.setChildren(allChild);

        // 树的第一层（零售和marketing）
        try {
            for(ActivityType type: allTypes){
                if("RO_HQ_1".equals(areaCode)){
                    if(MARKETING_CODE.equals(type.getCode())){
                        setNameForLeveOne(allTypes, allChild, type);
                    }
                } else if("RO_HQ_2".equals(areaCode)){
                    if(RETAIL_CODE.equals(type.getCode())){
                        setNameForLeveOne(allTypes, allChild, type);
                    }
                } else {
                    if(this.ACTIVITY_FIRST_LEVEL.equals(type.getLevel())){
                        setNameForLeveOne(allTypes, allChild, type);
                    }
                }

            }
        } catch (Exception e) {
            log.error("构造科目树异常");
            log.error(JSON.toJSONString(e));
            throw new RRException("constructionError");
        }
        return R.ok().put("data",list);
    }

    private void setNameForLeveOne(List<ActivityType> allTypes, List<ActivityType> allChild, ActivityType type) {
        List<ActivityType> lv1Child = new ArrayList<>(); // children
        if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
            type.setNameCn(type.getNameEn() + "-ALL");
        } else {
            type.setNameCn(type.getNameCn() + "-ALL");
        }
        allChild.add(type);
        // 给第一层加入children
        addLv1Children(lv1Child, type, allTypes);
    }

    // 科目第二层
    private void addLv1Children(List<ActivityType> lv1Child, ActivityType type, List<ActivityType> allTypes){
        type.setChildren(lv1Child);
        for(ActivityType element: allTypes){
            if(ACTIVITY_SECOND_LEVEL.equals(element.getLevel()) && element.getParentId().equals(type.getCode())){
                List<ActivityType> lv2Child = new ArrayList<>();
                if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                    element.setNameCn(element.getNameEn() + "-ALL");
                } else {
                    element.setNameCn(element.getNameCn() + "-ALL");
                }
                lv1Child.add(element);


                // 给第2层加入children
                addLv2Children(lv2Child, element, allTypes);
            }
        }
    }

    // 给第2层加入children
    private void addLv2Children(List<ActivityType> lv2Child, ActivityType type, List<ActivityType> allTypes){
        type.setChildren(lv2Child);
        for(ActivityType element: allTypes){
            if(ACTIVITY_THIRD_LEVEL.equals(element.getLevel()) && element.getParentId().equals(type.getCode())){
                if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                    element.setNameCn(element.getNameEn());
                }
                lv2Child.add(element);
            }
        }
    }
}
