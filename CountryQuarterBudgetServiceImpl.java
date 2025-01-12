package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.CountryMarketingBudgetDao;
import com.wiko.emarket.dao.CountryQuarterBudgetDao;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;
import com.wiko.emarket.entity.CountryQuarterBudgetEntity;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.CountryQuarterBudgetService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.RequestUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 15:51
 */
@Service
public class CountryQuarterBudgetServiceImpl extends ServiceImpl<CountryQuarterBudgetDao, CountryQuarterBudgetEntity> implements CountryQuarterBudgetService {
    @Resource
    private CountryQuarterBudgetDao dao;

    @Resource
    private CountryMarketingBudgetDao marketingBudgetDao;

    @Resource
    private AreaService areaService;


    @Override
    public String saveCountryBudget(CountryQuarterBudgetEntity entity, SysUserEntity user) {
        String checkRet = checkData(entity);
        if(!checkRet.equals("success")){
            return checkRet;
        }
        if(StringUtils.isNotBlank(entity.getUuid())) {
            EntityWrapper<CountryQuarterBudgetEntity> entityEntityWrapper = new EntityWrapper<>();
            entityEntityWrapper.eq("uuid",entity.getUuid());
            CountryQuarterBudgetEntity entity1 = selectOne(entityEntityWrapper);
            if(!entity1.getQuarterCode().equals(entity.getQuarterCode())){
                entity.setPreAmount(entity1.getSumAmount());
            }
            entity.setUpdatedDate(DateUtil.date());
            entity.setUpdatedBy(user.getUserId().toString());
            try {
                dao.updateById(entity);
                return "success";
            } catch (Exception e) {
                return "update data error";
            }
        } else {
            String uuid = IdUtil.simpleUUID();
            entity.setUpdatedDate(DateUtil.date());
            entity.setUpdatedBy(user.getUserId().toString());
            entity.setCreatedDate(DateUtil.date());
            entity.setCreatedBy(user.getUserId().toString());
            entity.setUuid(uuid);
            entity.setStatus("1");
            entity.setPreAmount(BigDecimal.ZERO);
            try {
                this.insert(entity);
                return "success";
            } catch (Exception e) {
                return "add data error";
            }

        }
    }

    @Override
    public BigDecimal getQuarterAmount(String code, String year) {
        Map<String,String> map = new HashMap<>();
        map.put("code",code);
        map.put("year",year);
        map.put("status","1");
        BigDecimal quarterAmount = dao.getQuarterAmount(map);
        return quarterAmount;
    }

    @Override
    public JSONObject getCountryQuarterBudget(String code, String year) {
        JSONObject object = new JSONObject();
        EntityWrapper<CountryMarketingBudgetEntity> entityEntityWrapper = new EntityWrapper<>();
        entityEntityWrapper.eq("status", "1").eq("year",year)
                .eq("area_code",code).eq("source","MBF");
        EntityWrapper<CountryQuarterBudgetEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("status", "1").eq("year",year)
                .eq("area_code",code).orderBy("sum_amount",false).last("limit 1");
        List<CountryMarketingBudgetEntity> countryMarketingBudgetEntities = marketingBudgetDao.selectList(entityEntityWrapper);
        BigDecimal calculate  = BigDecimal.ZERO;
        if(countryMarketingBudgetEntities.size() != 0){
            calculate = calculate(countryMarketingBudgetEntities);
        }
        CountryQuarterBudgetEntity entity = this.selectOne(wrapper);
        BigDecimal remainderAmount = BigDecimal.ZERO;
        List<AreaEntity> areaList = areaService.selectList(null);
        AreaEntity areaEntity = null;
        AreaEntity areaParent = null;
        for(AreaEntity areaEntity1:areaList){
            if(areaEntity1.getCode().equals(code)){
                areaEntity = areaEntity1;
                break;
            }
        }
        for(AreaEntity temp:areaList){
            if(temp.getCode().equals(areaEntity.getParentId())){
                areaParent = temp;
                break;
            }
        }
        if(null != entity){
            remainderAmount = calculate.subtract(entity.getSumAmount());
            object = (JSONObject)JSONObject.toJSON(entity);
            object.put("remainderAmount",remainderAmount);
        } else{
            object.put("areaCode", code);
            object.put("quarterCode", "Q1(1-3月)");
            object.put("remainderAmount",calculate);
        }
        object.put("parentCode",areaEntity.getParentId());
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        object.put("parentCodeName", isChinese ? areaParent.getNameCn() : areaParent.getNameEn());
        object.put("areaCodeName", isChinese ? areaEntity.getNameCn() : areaParent.getNameEn());
        object.put("amount", calculate);
        object.put("year",year);
        object.put("source","MBF");
        return object;
    }

    public BigDecimal calculate(List<CountryMarketingBudgetEntity> marketingBudgetEntities){
        BigDecimal sum = BigDecimal.ZERO;
        for(CountryMarketingBudgetEntity entity: marketingBudgetEntities) {
            sum = sum.add(entity.getAmount());
        }
        return sum;
    }

    private String checkData(CountryQuarterBudgetEntity entity) {
        String retString = "success";
        EntityWrapper<CountryQuarterBudgetEntity> entityEntityWrapper = new EntityWrapper<>();
        entityEntityWrapper.eq("status", "1").eq("year",entity.getYear())
                        .eq("area_code",entity.getAreaCode());
        List<CountryQuarterBudgetEntity> list = dao.selectList(entityEntityWrapper);
        if(list.size() > 1){
            return I18nUtil.getMessage("quarterlyBudgetDuplicate");
        } else {
            //只能向下修改，比如list中是第2季度的预算，entity的数据不能是第一季度的数据
            int quarter = Integer.parseInt(entity.getQuarterCode().substring(1,2));
            if(null != list && list.size() != 0 ){
                int listQuarter = Integer.parseInt(list.get(0).getQuarterCode().substring(1,2));
                // 校验修改季度必选大于等于修改前的季度
                if(quarter<listQuarter){
                    return I18nUtil.getMessage("quarterlyBudgetSmaller");
                }
                if(quarter !=listQuarter){
                        // 校验修改季度预算金额必选大于等于修改前的季度预算金额
                        if(list.get(0).getSumAmount().compareTo(entity.getSumAmount()) > 0){
                            return I18nUtil.getMessage("quarterlyBudgetLess");
                        }
                }else {
                    if(null != list.get(0).getPreAmount()&& list.get(0).getPreAmount().compareTo(entity.getSumAmount()) > 0){
                        return I18nUtil.getMessage("quarterlyBudgetLess");
                    }
                }
            }

//            switch (quarter){
//                case 1:
//                    //当为Q1季度时如果存在Q2季度则Q1季度必须小于等于Q2
//                    nextCode = "Q2(1-6)";
//                    if(compareAmount(list,nextCode, entity.getSumAmount())){
//                        retString = "第1季度预算金额不应该大于第2季度卷积，请修改";
//                    }
//                    break;
//                case 2:
//                    preCode = "Q1(1-3)";
//                    nextCode = "Q3(1-9)";
//                    if(compareAmount(list,nextCode, entity.getSumAmount())){
//                        retString = "第2季度预算金额不应该大于第3季度卷积，请修改";
//                    }
//                    if(isExist(preCode,list)){
//                        retString = "第2季度预算必须要存在第1季度";
//                    }
//
//                    if(!compareAmount(list,preCode, entity.getSumAmount())){
//                        retString = "第1季度预算金额不应该大于第2季度卷积，请修改";
//                    }
//                    break;
//                case 3:
//                    //2,3季度需要比上一季度大，下一季度小
//                    preCode = "Q2(1-6)";
//                    nextCode = "Q4(1-12)";
//                    if(compareAmount(list,nextCode, entity.getSumAmount())){
//                        retString = "第3季度预算金额不应该大于第4季度卷积，请修改";
//                    }
//                    if(isExist(preCode,list)){
//                        retString = "第3季度预算必须要存在第2季度";
//                    }
//                    if(!compareAmount(list,preCode, entity.getSumAmount())){
//                        retString = "第2季度预算金额不应该大于第3季度卷积，请修改";
//                    }
//                    break;
//                case 4:
//                    preCode = "Q3(1-9)";
//                    if(!compareAmount(list,preCode, entity.getSumAmount())){
//                        retString = "第3季度预算金额不应该大于第4季度卷积，请修改";
//                    }
//                    break;
//            }
//            if(!retString.equals("success") ){
//                return retString;
//            }
            // 季度卷积不大于国家总包
            // 1 通过year和code获取国家总资金
            Map<String,String> map = new HashMap<>();
            map.put("year", entity.getYear());
            map.put("status", "1");
            map.put("level", "3");
            map.put("code", entity.getAreaCode());
            map.put("source", "MBF");
            BigDecimal bigDecimal = marketingBudgetDao.getCountryBudgetSumByArea(map);
            if(null == bigDecimal) {
                return I18nUtil.getMessage("officeNotExist");
            }
            // 2 找出现有所有最大数据
            if(entity.getSumAmount().compareTo(bigDecimal) > 0){
                return I18nUtil.getMessage("overOffice");
            }
        }

        return retString;
    }

//    public CountryQuarterBudgetEntity getMaxData(List<CountryQuarterBudgetEntity> list, CountryQuarterBudgetEntity entity){
//        CountryQuarterBudgetEntity maxEntity = new CountryQuarterBudgetEntity();
//        maxEntity = entity;
//        for(CountryQuarterBudgetEntity temp: list){
//            if(temp.getSumAmount().compareTo(maxEntity.getSumAmount()) > 0){
//                maxEntity = temp;
//            }
//        }
//        return maxEntity;
//    }

    public boolean isExist(String preCode,List<CountryQuarterBudgetEntity> list){
        for(CountryQuarterBudgetEntity entity:list){
            if(entity.getQuarterCode().equals(preCode)){
                return true;
            }
        }
        return false;
    }

    public boolean compareAmount(List<CountryQuarterBudgetEntity> list, String code, BigDecimal amount){
        for(CountryQuarterBudgetEntity entity: list){
            if(entity.getQuarterCode().equals(code)){
                return entity.getSumAmount().compareTo(amount) < 0;
            }
        }
        return false;
    }
}
