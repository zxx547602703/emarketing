package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.CountryMarketingBudgetDao;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.BudgetTypeService;
import com.wiko.emarket.service.campaign.CountryMarketingBudgetService;
import com.wiko.emarket.service.campaign.CountryQuarterBudgetService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.RequestUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 15:51
 */
@Service
public class CountryMarketingBudgetServiceImpl extends ServiceImpl<CountryMarketingBudgetDao, CountryMarketingBudgetEntity> implements CountryMarketingBudgetService {

    public static String NORMAL_STATUS = "1";
    @Resource
    private BudgetTypeService budgetTypeService;

    public Map<String, String> budgetTypeMap = new HashMap<>();

    @Resource
    private CountryMarketingBudgetDao dao;

    @Resource
    private AreaService areaService;

    @Resource
    private CountryQuarterBudgetService countryQuarterBudgetService;

    @Override
    public String saveCountryBudget(CountryBudgetEntity countryBudgetEntity, SysUserEntity user) {
        //校验入参是否存在相同类型预算
        this.budgetTypeMap = changeListToMap();
        String checkInputParamResult = checkInputParam(countryBudgetEntity);
        if (!"success".equals(checkInputParamResult)) {
            return checkInputParamResult;
        }

        //构造不同类型的预算
        List<CountryMarketingBudgetEntity> marketingBudgetEntities = constructMarketingBudgetEntities(countryBudgetEntity);
        Map<String, CountryMarketingBudgetEntity> updateMap = new HashMap<>(); //存放需要修改的预算，key是修改数据uuid;
        Map<String, CountryMarketingBudgetEntity> insertMap = new HashMap<>(); //存放需要插入的预算
        //校验重复
        StringBuilder builder = new StringBuilder();
        for (CountryMarketingBudgetEntity marketingBudgetEntity : marketingBudgetEntities) {
            Map<String, String> checkDuplicateMap = checkDuplicate(marketingBudgetEntity);
            for (Map.Entry<String, String> entry : checkDuplicateMap.entrySet()) {
                if (entry.getKey().equals("3")) { //数据异常，数据库同类型的预算存在多条记录
                    builder.append(entry.getValue());
                } else if (entry.getKey().equals("1")) { // 修改操作
                    updateMap.put(entry.getValue(), marketingBudgetEntity);
                } else if (entry.getKey().equals("2")) {
                    continue;
                } else {
                    insertMap.put(IdUtil.simpleUUID(), marketingBudgetEntity);
                }
            }
        }
        if (builder.length() != 0) {
            return builder.toString();
        }
        String feeSumResult = null;
        //检验代表处级某个类型预算总和小于等于该代表处所属地区级该类型预算
        feeSumResult = CompareAreaSum(insertMap, updateMap, countryBudgetEntity.getAreaCode());
        if (!"success".equals(feeSumResult)) {
            return feeSumResult;
        }
        //对修改的数据做修改操作，对新增的数据做插入操作
        operateDate(insertMap, updateMap, user);
        return "success";
    }

    @Override
    public JSONArray getCountryBudget(String code, String year) {
        JSONArray array = new JSONArray();
        EntityWrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("code", code);
        String parentCode = areaService.selectOne(wrapper).getParentId();
        EntityWrapper<CountryMarketingBudgetEntity> budgetWrapper = new EntityWrapper<>();
        budgetWrapper.eq("status", "1").eq("year", year).eq("area_code", code);
        List<CountryMarketingBudgetEntity> marketingBudgetEntities = this.selectList(budgetWrapper);
        for (CountryMarketingBudgetEntity entity : marketingBudgetEntities) {
            JSONObject jsonObject = (JSONObject) JSONObject.toJSON(entity);
            jsonObject.put("parentCode", parentCode);
            array.add(jsonObject);
        }
        return array;
    }

    @Override
    public String delCountryBudget(Map<String, Object> map) {
        List<String> uuids = (List<String>) map.get("uuids");
        String code = (String) map.get("code");
        String year = (String) map.get("year");
        String ret = checkData(uuids, code, year);
        if (ret.equals("success")) {
            EntityWrapper<CountryMarketingBudgetEntity> wrapper = new EntityWrapper<>();
            wrapper.in("uuid", uuids);
            this.delete(wrapper);
            return "success";
        } else {
            return ret;
        }

    }

    private String checkData(List<String> uuids, String code, String year) {
        String checkDataRet = "success";
        Wrapper<CountryMarketingBudgetEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("area_code", code).eq("year", year).notIn("uuid", uuids);
        List<CountryMarketingBudgetEntity> countryMarketingBudgetEntities = this.selectList(wrapper);
        BigDecimal sum = BigDecimal.ZERO;
        if (null != countryMarketingBudgetEntities && countryMarketingBudgetEntities.size() > 0) {
            for (CountryMarketingBudgetEntity entity : countryMarketingBudgetEntities) {
                sum = sum.add(entity.getAmount());
            }
        }
        Wrapper<CountryQuarterBudgetEntity> countryQuarterBudgetEntityWrapper = new EntityWrapper<>();
        countryQuarterBudgetEntityWrapper.eq("area_code", code).eq("year", year);
        List<CountryQuarterBudgetEntity> countryQuarterBudgetEntities = countryQuarterBudgetService.selectList(countryQuarterBudgetEntityWrapper);
        if (countryQuarterBudgetEntities.size() > 0 && (countryQuarterBudgetEntities.get(0).getSumAmount().compareTo(sum) > 0)) {
            checkDataRet = I18nUtil.getMessage("totalBudgetNotEnough");
        }
        return checkDataRet;
    }

    public void operateDate(Map<String, CountryMarketingBudgetEntity> insertMap, Map<String, CountryMarketingBudgetEntity> updateMap, SysUserEntity user) {
        for (Map.Entry<String, CountryMarketingBudgetEntity> entry : insertMap.entrySet()) {
            CountryMarketingBudgetEntity value = entry.getValue();
            value.setCreatedDate(DateUtil.date());
            value.setUpdatedDate(DateUtil.date());
            value.setCreatedBy(user.getUserId().toString());
            value.setUpdatedBy(user.getUserId().toString());
            this.insert(value);
        }
        for (Map.Entry<String, CountryMarketingBudgetEntity> entry : updateMap.entrySet()) {
            CountryMarketingBudgetEntity value = entry.getValue();
            value.setUpdatedDate(DateUtil.date());
            value.setUpdatedBy(user.getUserId().toString());
            dao.updateById(entry.getValue());
        }
    }

    public List<CountryMarketingBudgetEntity> constructMarketingBudgetEntities(CountryBudgetEntity countryBudgetEntity) {
        List<CountryMarketingBudgetEntity> list = new ArrayList<>();
        List<TypeAmountEntity> typeAmountEntities = countryBudgetEntity.getTypeAmountEntities();
        for (TypeAmountEntity entity : typeAmountEntities) {
            CountryMarketingBudgetEntity marketingBudgetEntity = constructMarketingBudgetEntity(countryBudgetEntity, entity);
            list.add(marketingBudgetEntity);
        }
        return list;
    }

    public CountryMarketingBudgetEntity constructMarketingBudgetEntity(CountryBudgetEntity countryBudgetEntity, TypeAmountEntity entity) {
        CountryMarketingBudgetEntity marketingBudgetEntity = new CountryMarketingBudgetEntity();
        marketingBudgetEntity.setYear(countryBudgetEntity.getYear());
        marketingBudgetEntity.setLevel(countryBudgetEntity.getLevel());
        marketingBudgetEntity.setAreaCode(countryBudgetEntity.getCtyCode());
        marketingBudgetEntity.setSource(countryBudgetEntity.getSource());
        marketingBudgetEntity.setBudgetTypeCode(entity.getType());
        marketingBudgetEntity.setStatus(NORMAL_STATUS);
        marketingBudgetEntity.setAmount(entity.getAmount());
        marketingBudgetEntity.setSource(entity.getSource());
        return marketingBudgetEntity;
    }

    public String checkInputParam(CountryBudgetEntity countryBudgetEntity) {
        List<TypeAmountEntity> list = countryBudgetEntity.getTypeAmountEntities();
        if (null == list || list.size() == 0) {
            return I18nUtil.getMessage("needConfigBudgetType");
        }
        Set<String> set = new HashSet<>();
        for (TypeAmountEntity entity : list) {
            if (!set.contains(entity.getType())) {
                set.add(entity.getType());
            } else {
                return I18nUtil.getMessage("DuplicateBudgetType");
            }
        }
        return "success";
    }

    public String CompareAreaSum(Map<String, CountryMarketingBudgetEntity> insertMap, Map<String, CountryMarketingBudgetEntity> updateMap, String areaCode) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, CountryMarketingBudgetEntity> entry : insertMap.entrySet()) {
            CountryMarketingBudgetEntity entity = entry.getValue();
            Map<String, String> map = getComparedMsg(entity, areaCode);
            if (map.containsKey("1")) {
                entity.setParentId(map.get("1"));
                entity.setUuid(entry.getKey());
            } else {
                builder.append(map.get("2"));
            }
        }

        for (Map.Entry<String, CountryMarketingBudgetEntity> entry : updateMap.entrySet()) {
            CountryMarketingBudgetEntity entity = entry.getValue();
            entity.setUuid(entry.getKey());
            Map<String, String> map = getComparedMsg(entity, areaCode);
            if (map.containsKey("1")) {
                entity.setParentId(map.get("1"));
                entity.setUuid(entry.getKey());
            } else {
                builder.append(map.get("2"));
            }
        }
        if (builder.length() == 0) {
            builder.append("success");
        }
        return builder.toString();
    }

    public Map<String, String> getComparedMsg(CountryMarketingBudgetEntity entity, String areaCode) {
        Map<String, String> map = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        CountryMarketingBudgetEntity marketingBudgetEntity = new CountryMarketingBudgetEntity();
        BeanUtil.copyProperties(entity, marketingBudgetEntity);
        marketingBudgetEntity.setLevel("2");
        marketingBudgetEntity.setAreaCode(areaCode);
        List<CountryMarketingBudgetEntity> areaBudgetList = getAreaBudget(marketingBudgetEntity);
        if (null == areaBudgetList || areaBudgetList.size() == 0) {
            // 设置国际化
            builder.append(String.format(I18nUtil.getMessage("pleaseConfigArea"), budgetTypeMap.get(entity.getBudgetTypeCode()), budgetTypeMap.get(entity.getBudgetTypeCode())));

        } else if (areaBudgetList.size() == 1) {
            CountryMarketingBudgetEntity queryObject = new CountryMarketingBudgetEntity();
            BeanUtil.copyProperties(entity, queryObject);
            queryObject.setParentId(areaBudgetList.get(0).getUuid());
            BigDecimal countryBudgetSum = getCountryBudgetSum(queryObject);
            if (null == countryBudgetSum) {
                countryBudgetSum = BigDecimal.ZERO;
            }
            BigDecimal countrySum = countryBudgetSum.add(entity.getAmount());
            if (countrySum.compareTo(areaBudgetList.get(0).getAmount()) > 0) {
                // 设置国际化
                builder.append(String.format(I18nUtil.getMessage("overAreaBudget"), budgetTypeMap.get(entity.getBudgetTypeCode())));

            }
        } else {
            // 设置国际化
            builder.append(String.format(I18nUtil.getMessage("areaDuplicateType")
                    , budgetTypeMap.get(entity.getBudgetTypeCode())));
        }
        if (builder.length() == 0) {
            map.put("1", areaBudgetList.get(0).getUuid());
        } else {
            map.put("2", builder.toString());
        }
        return map;
    }

    public Map<String, String> checkDuplicate(CountryMarketingBudgetEntity entity) {
        Map<String, String> map = new HashMap<>();
        EntityWrapper<CountryMarketingBudgetEntity> entityEntityWrapper = new EntityWrapper<>();
        entityEntityWrapper.eq("year", entity.getYear())
                .eq("level", entity.getLevel())
                .eq("source", entity.getSource())
                .eq("status", entity.getStatus())
                .eq("area_code", entity.getAreaCode())
                .eq("budget_type_code", entity.getBudgetTypeCode());
        List<CountryMarketingBudgetEntity> queryList = this.selectList(entityEntityWrapper);
        if (queryList.size() == 0) {
            map.put("0", "success");
        } else if (queryList.size() == 1) {
            if (entity.equals(queryList.get(0))) {
                //2与原数据相同
                map.put("2", queryList.get(0).getUuid());

            } else {
                map.put("1", queryList.get(0).getUuid()); //1与原数据不相同
            }

        } else {
            map.put("3", String.format(I18nUtil.getMessage("recordDuplicate"), budgetTypeMap.get(entity.getBudgetTypeCode())));
        }
        return map;
    }

    public Map<String, String> changeListToMap() {
        List<BudgetTypeEntity> list = budgetTypeService.selectList(null);
        Map<String, String> map = new HashMap<>();

        for (BudgetTypeEntity entity : list) {
            map.put(entity.getCode(), CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang())?entity.getNameCn():entity.getNameEn());
        }
        return map;
    }


    /**
     * 查询某个地区部中所有代表处总和
     *
     * @param entity
     * @return
     */
    @Override
    public BigDecimal getCountryBudgetSum(CountryMarketingBudgetEntity entity) {
        BigDecimal areaBudget = dao.getCountryBudgetSum(entity);
        return areaBudget;
    }

    /**
     * 查询单个代表处预算
     *
     * @param uuid
     * @return
     */
    @Override
    public CountryMarketingBudgetEntity getCountryBudget(String uuid) {

        return null;
    }

    /**
     * 查询单个地区预算
     *
     * @param marketingBudgetEntity
     * @return
     */
    @Override
    public List<CountryMarketingBudgetEntity> getAreaBudget(CountryMarketingBudgetEntity marketingBudgetEntity) {
        return dao.getAreaBudget(marketingBudgetEntity);
    }

}
