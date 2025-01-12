package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysConfigService;
import com.wiko.emarket.constant.AreaLevelEnum;
import com.wiko.emarket.constant.CampaignStatusEnum;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.AreaDao;
import com.wiko.emarket.dao.BudgetDao;
import com.wiko.emarket.dao.CountryMarketingBudgetDao;
import com.wiko.emarket.dao.SysUserAreaDao;
import com.wiko.emarket.dto.CArea;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;
import com.wiko.emarket.entity.SysUserArea;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.BudgetTypeService;
import com.wiko.emarket.service.campaign.CountryQuarterBudgetService;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.marketingBudget.AmountVO;
import com.wiko.psi.dao.CountryMapper;
import com.wiko.psi.entity.Country;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:52
 */
@Service
@Slf4j
public class AreaServiceImpl extends ServiceImpl<AreaDao, AreaEntity> implements AreaService {
    public static final String GLOBAL_AREA_CODE = "01";
    @Resource
    private AreaDao dao;

    @Resource
    private CountryMarketingBudgetDao marketingBudgetDao;

    @Resource
    private CountryQuarterBudgetService service;

    @Resource
    private SysUserAreaDao sysUserAreaDao;

    @Resource
    private BudgetDao budgetDao;
    @Resource
    private CountryMapper countryMapper;
    @Resource
    private BudgetTypeService budgetTypeService;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public JSONObject getAreaTree(String code,String level) {
        List<AreaEntity> areaEntities = dao.selectList(null);
        if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
            for (AreaEntity area : areaEntities
            ) {
                area.setNameCn(area.getNameEn());
            }
        }
        AreaEntity areaEntity = getListArea(code, areaEntities);
        if (null == areaEntity) {
            return null;
        }
        JSONObject object = getChildren(areaEntity, areaEntities,level);
        return object;
    }

    @Override
    public JSONArray getAreaTreeForSubmiter(Long userId){
        JSONArray array = new JSONArray();
        Wrapper<SysUserArea> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("level","3");
        List<SysUserArea> sysUserAreas = sysUserAreaDao.selectList(wrapper);
        List<String> collect = sysUserAreas.stream().map(SysUserArea::getAreaCode).collect(Collectors.toList());
        Wrapper<AreaEntity> wrapper1 = new EntityWrapper<>();
        wrapper1.in("code",collect);
        List<AreaEntity> areaEntities = dao.selectList(wrapper1);
        Set<String> set = new HashSet<>();
        for(AreaEntity entity: areaEntities){
            set.add(entity.getParentId());
        }
        Wrapper<AreaEntity> wrapper2 = new EntityWrapper<>();
        wrapper2.in("code",set);
        List<AreaEntity> rgAreaEntities = dao.selectList(wrapper2);
        for(AreaEntity areaEntity : rgAreaEntities){
            JSONObject obj = new JSONObject();
            obj.put("value",areaEntity.getCode());
            if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                obj.put("label",areaEntity.getNameEn());
            } else {
                obj.put("label",areaEntity.getNameCn());
            }
            JSONArray child = new JSONArray();
            obj.put("children",child);
            array.add(obj);
            for(AreaEntity entity : areaEntities){
                if(entity.getParentId().equals(areaEntity.getCode())){
                    JSONObject object = new JSONObject();
                    object.put("value",entity.getCode());
                    if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                        object.put("label",entity.getNameEn());
                    } else {
                        object.put("label",entity.getNameCn());
                    }
                    child.add(object);
                }
            }
        }
        return array;
    }

    @Override
    public JSONObject getBudgetOrg(String rgCode){
        JSONObject object = new JSONObject();
        List<String> list = new ArrayList<>();
        list.add(rgCode);
        list.add("RG_HQ");
        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.in("code",list);
        List<AreaEntity> areaList = this.selectList(wrapper);
        object.put("rgCode",areaList);
        return object;
    }

    @Override
    public JSONObject getBudgetRO(String rgCode, String submitterRo) {
        JSONObject object = new JSONObject();
        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        if(submitterRo.contains("HQ")){
            wrapper.eq("code", submitterRo);
        } else{
            if(rgCode.contains("HQ")){
                wrapper.eq("parent_id",rgCode);
            }else{
                wrapper.eq("code", submitterRo);
            }
        }
        List<AreaEntity> areaList = this.selectList(wrapper);
        object.put("roCode",areaList);
        return object;
    }


    @Override
    public List<BudgetTypeEntity> getBugetType(String roCode, String submiterRoCode){
        List<String> budgets = new ArrayList<>();
        if(submiterRoCode.equals("RO_HQ_1")){
            budgets.add("BM");
            budgets.add("CM");
        } else if(submiterRoCode.equals("RO_HQ_2")){
            budgets.add("RM");
            budgets.add("SM");
        }else{
            if(roCode.equals("RO_HQ_1")){
                budgets.add("CM");
            }else if(roCode.equals("RO_HQ_2")){
                budgets.add("SM");
            }else {
                budgets.add("RM");
                budgets.add("PM");
                budgets.add("BM");
            }
        }
        Wrapper<BudgetTypeEntity> wrapper = new EntityWrapper<>();
        wrapper.in("code",budgets);
        List<BudgetTypeEntity> budgetTypeEntities = budgetTypeService.selectList(wrapper);
        return budgetTypeEntities;
    }

    @Override
    public List<String> getBugetYear(){
        String yearStr = sysConfigService.getValue("BUGET_YEAR");
        if (StringUtils.isEmpty(yearStr))  {
            return new ArrayList<>();
        }
        return Arrays.asList(yearStr.split(","));
    }

    @Override
    public List<AreaEntity> getBudgetCountry(String roCode){
        List<AreaEntity> areaList;
        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("parent_id",roCode);
        areaList = this.selectList(wrapper);
        return areaList;
    }

    @Override
    public JSONObject getUserAreaTree(Long userId) {
        JSONObject retJson = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            Wrapper<SysUserArea> wrapper = new EntityWrapper<>();
            wrapper.eq("user_id", userId);
            List<SysUserArea> sysUserAreas = sysUserAreaDao.selectList(wrapper);
            List<AreaEntity> areaEntityList = dao.selectList(null);
            List<AreaEntity> areaEntities = new ArrayList<>();
            List<AreaEntity> roAreas = new ArrayList<>();
            for(SysUserArea userArea: sysUserAreas){
                for(AreaEntity area : areaEntityList){
                    if(userArea.getAreaCode().equals(area.getCode())){
                        if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                            area.setNameCn(area.getNameEn());
                        }
                        areaEntities.add(area);
                        if(area.getLevel().equals("3")){
                            roAreas.add(area);
                        }
                    }
                }
            }
            for(AreaEntity area: areaEntities){
                if(area.getLevel().equals("1")){
                    jsonArray.add(getAreaTree("01","3"));
                    break;
                } else if(area.getLevel().equals("2")){
                   JSONObject object = getChildren(area, areaEntities,"3");
                   jsonArray.add(object);
                   for(int i = 0; i<roAreas.size();i++){
                       if(area.getCode().equals(roAreas.get(i).getParentId())){
                           roAreas.remove(roAreas.get(i));
                           i--;
                       }
                   }
                }
            }
            if(roAreas.size() !=0){
                for(AreaEntity entity: roAreas){
                    JSONObject object = new JSONObject();
                    object.put("code", entity.getCode());
                    object.put("nameCn", entity.getNameCn());
                    object.put("level", entity.getLevel());
                    jsonArray.add(object);
                }
            }
        } catch (Exception e) {
            log.info("get user area error");
            log.info(JSON.toJSONString(e));
            retJson.put("code", 500);
            retJson.put("msg", "get user area error");
        }
        retJson.put("code", 0);
        retJson.put("data",jsonArray);
        retJson.put("msg", "success");
        return retJson;
    }


    @Override
    public JSONArray getAreaFee(String code, String year) {
        JSONArray array = new JSONArray();
//        if ("01".equals(code)) {
//            array.add(addMsfFee(year));
//        }
        EntityWrapper<CountryMarketingBudgetEntity> budgetWrapper = new EntityWrapper<>();
        budgetWrapper.eq("status", "1").eq("year", year);
        List<CountryMarketingBudgetEntity> marketingBudgetEntities = marketingBudgetDao.selectList(budgetWrapper);
        EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
        areaWrapper.in("level", "1,2,3");
        List<AreaEntity> areaEntities = dao.selectList(null);
        for (AreaEntity  area:areaEntities) {
            if(CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())){
                area.setNameCn(area.getNameEn());
            }
        }
        AreaEntity areaEntity = getListArea(code, areaEntities);
        if (null == areaEntity) {
            return null;
        }
        JSONObject object = getFeeChildren(areaEntity, areaEntities, marketingBudgetEntities, year);
        array.add(object);
        return array;
    }

    @Override
    public List<AreaEntity> getAllArea() {
        List<AreaEntity> list = this.selectList(null);
        return list;
    }

    @Override
    public List<AreaEntity> getAllAreaCode(String code) {
        List<AreaEntity> list = new ArrayList<>();
        List<AreaEntity> areaEntities = dao.selectList(null);
        for (AreaEntity entity : areaEntities) {
            if (entity.getCode().equals(code)) {
                if (!entity.getLevel().equals("4")) {
                    list.addAll(getChildrenCode(areaEntities, entity));
                } else {
                    list.add(entity);
                }
            }
        }
        return list;
    }

    @Override
    public JSONArray getAllAreaFeeTree(String year, Long userId) {
        if (StringUtils.isBlank(year)) {
            year = String.valueOf(DateUtil.year(DateUtil.date()));
        }
        JSONArray array = new JSONArray();
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.eq("user_id", userId);
        List<SysUserArea> list = sysUserAreaDao.selectList(wrapper);
        boolean isEnglish = CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang());
        Set<String> lv1Set = new HashSet<>();
        Set<String> lv2Set = new HashSet<>();
        Set<String> lv3Set = new HashSet<>();
        for (SysUserArea sysUserArea : list) {
            if (sysUserArea.getLevel().equals("1")) {
                lv1Set.add(sysUserArea.getAreaCode());
                break;
            }
            if (sysUserArea.getLevel().equals("2")) {
                lv2Set.add(sysUserArea.getAreaCode());
                continue;
            }
            if (sysUserArea.getLevel().equals("3")) {
                lv3Set.add(sysUserArea.getAreaCode());
                continue;
            }
        }
        if (lv1Set.size() != 0) {
//            array.add(addMsfFee(year));
            array.addAll(getAreaFee("01", year));
            return array;
        }
        if (lv2Set.size() != 0) {
            JSONArray array1 = getArray(lv2Set, lv3Set, year);
            array.addAll(array1);
            return array;
        }

        return getArrayByLv3(lv3Set, year);
    }

//    public JSONObject addMsfFee(String year) {
//        EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
//        areaWrapper.in("code", "01");
//        AreaEntity areaEntity = this.selectOne(areaWrapper);
//        EntityWrapper<CountryMarketingBudgetEntity> budgetWrapper = new EntityWrapper<>();
//        budgetWrapper.eq("status", "1").eq("year", year).eq("source", "MSF");
//        List<CountryMarketingBudgetEntity> marketingBudgetEntities = marketingBudgetDao.selectList(budgetWrapper);
//        JSONObject object = getJsonObject(areaEntity, year, "MSF");
//        BigDecimal amount=BigDecimal.ZERO;
//        if (null != marketingBudgetEntities && marketingBudgetEntities.size() > 0) {
//            for (CountryMarketingBudgetEntity marketingBudgetEntity:marketingBudgetEntities ) {
//                amount=amount.add(marketingBudgetEntity.getAmount());
//            }
//        }
//        object.put("amount", amount);
//        BigDecimal benefitAmount = getBenifit(year, areaEntity.getCode(), areaEntity.getLevel(), "MSF");
//        object.put("benefitAmount", benefitAmount);
//        object.put("quarterAmount", BigDecimal.ZERO);
//        object.put("remainderAmount", BigDecimal.ZERO);
//        object.put("id", object.get("id") + "-1");
//        return object;
//    }


    @Override
    public List<AreaEntity> getAllAreaList(Long userId, String level) {
        List<AreaEntity> retList = new ArrayList<>();
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.eq("user_id", userId);
        List<SysUserArea> list = sysUserAreaDao.selectList(wrapper);
        
        EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
        areaWrapper.in("level","1,2,3");
        List<AreaEntity> areaEntities = this.selectList(areaWrapper);
        for (AreaEntity area : areaEntities) {
            if (RequestUtil.getLang().equals(CommonConstant.EN_LANGUAGE)) {
                area.setNameCn(area.getNameEn());
            }
        }
        Map<String, AreaEntity> collect = areaEntities.stream().collect(Collectors.toMap(AreaEntity::getCode, areaEntity -> areaEntity));

        for (SysUserArea sysUserArea : list) {
            if (level.equals("2")) {
                if ("1,2".contains(sysUserArea.getLevel())) {
                    retList.add(collect.get(sysUserArea.getAreaCode()));
                }
            } else if (level.equals("3")) {
                if ("1,2,3".contains(sysUserArea.getLevel())) {
                    retList.add(collect.get(sysUserArea.getAreaCode()));
                }
            }
        }
        return retList;
    }

    @Override
    public JSONArray getSingleAreaFee(String code, String year) {
        JSONArray array = new JSONArray();
        EntityWrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
        areaWrapper.eq("code", code);
        AreaEntity areaEntity = this.selectOne(areaWrapper);

        EntityWrapper<AreaEntity> areaWrapper1 = new EntityWrapper<>();
        areaWrapper1.eq("code", areaEntity.getParentId());
        AreaEntity parentAreaEntity = this.selectOne(areaWrapper1);

        JSONObject parentObject = getObject(parentAreaEntity, year, parentAreaEntity.getCode(), "MBF");
        array.add(parentObject);

        if(code.contains("HQ")) {
            JSONObject parentObjectMsf = getObject(parentAreaEntity, year, parentAreaEntity.getCode(), "MSF");
            array.add(parentObjectMsf);
        }
        JSONObject object = getObject(areaEntity, year, code,"MBF");
        array.add(object);
        if(code.contains("HQ")){
            JSONObject objectMsf = getObject(areaEntity, year, code,"MSF");
            array.add(objectMsf);
        }

        return array;
    }

    public JSONObject getObject(AreaEntity areaEntity, String year, String code, String source) {
        EntityWrapper<CountryMarketingBudgetEntity> budgetWrapper = new EntityWrapper<>();
        budgetWrapper.eq("status", "1").eq("year", year).eq("area_code", code);
        List<CountryMarketingBudgetEntity> marketingBudgetEntities = marketingBudgetDao.selectList(budgetWrapper);
        JSONObject object = getJsonObject(areaEntity, year, source);
        BigDecimal calculate = calculate(marketingBudgetEntities, areaEntity.getCode(), source); //预算总金额
        object.put("amount", calculate);
        BigDecimal benefitAmount = getBenifit(year, code, areaEntity.getLevel(), source);
        if (null == benefitAmount) {
            benefitAmount = BigDecimal.ZERO;
        }
        object.put("benefitAmount", benefitAmount);

        // 季度累计/预算授予($)
        BigDecimal quarterAmount = null;
        if ("MSF".equalsIgnoreCase(source)) {
            // MSF是没有季度预算的， 因为季度预算下拉选只有MBF选项， 所以MSF季度预算直接设值0
            quarterAmount = BigDecimal.ZERO;
        } else {
            quarterAmount = getQuarterAmount(areaEntity.getCode(), year, areaEntity.getLevel());
            quarterAmount = (null == quarterAmount) ? BigDecimal.ZERO : quarterAmount;
        }
        object.put("quarterAmount", quarterAmount);

        if ("MSF".equalsIgnoreCase(source)) {
            // MSF 季度累计预算剩余都设置成0
            object.put("remainderAmount", BigDecimal.ZERO);
        } else {
            // 季度累计预算剩余 = 总预算 - 已授予的季度
            object.put("remainderAmount", calculate.subtract(quarterAmount));
        }


        return object;
    }

    public JSONArray getArray(Set<String> lv2Set, Set<String> lv3Set, String year) {
        JSONArray array = new JSONArray();
        for (String code : lv2Set) {
            EntityWrapper<AreaEntity> entityEntityWrapper = new EntityWrapper<>();
            entityEntityWrapper.eq("parent_id", code);
            List<AreaEntity> areaEntities = selectList(entityEntityWrapper);

            // lv2中有3了， 会一并给找了， 所有下面的LV3去掉， 不用再找了
            areaEntities.stream().forEach(areaEntity -> {
                if (lv3Set.contains(areaEntity.getCode())) {
                    lv3Set.remove(areaEntity.getCode());
                }
            });
            JSONArray jsonArray = getAreaFee(code, year);
            array.addAll(jsonArray);
        }

        // 添加Level3的
        array.addAll(getArrayByLv3(lv3Set, year));

        return array;
    }

    public JSONArray getArrayByLv3(Set<String> lv3Set, String year) {
        JSONArray array = new JSONArray();
        for (String code : lv3Set) {
            JSONArray jsonArray = getAreaFee(code, year);
            array.addAll(jsonArray);
        }

        return array;
    }

    public List<AreaEntity> getChildrenCode(List<AreaEntity> areaEntities, AreaEntity entity) {
        List<AreaEntity> list = new ArrayList<>();
        if (entity.getLevel().equals("4")) {
            list.add(entity);
            return list;
        } else {
            for (AreaEntity areaEntity : areaEntities) {
                if (StringUtils.isNotBlank(areaEntity.getParentId()) && areaEntity.getParentId().equals(entity.getCode())) {
                    List<AreaEntity> childrenCode = getChildrenCode(areaEntities, areaEntity);
                    list.addAll(childrenCode);
                }
            }
        }
        return list;
    }

    public AreaEntity getListArea(String code, List<AreaEntity> areaEntities) {
        for (AreaEntity entity : areaEntities) {
            if (entity.getCode().equals(code)) {
                return entity;
            }
        }
        return null;
    }

    public JSONObject getJsonObject(AreaEntity areaEntity, String year, String source) {
        JSONObject object = new JSONObject();
        object.put("id", areaEntity.getId());
        object.put("source", source);
        object.put("parentCode", areaEntity.getParentId());
        object.put("year", year);
        object.put("code", areaEntity.getCode());
        object.put("nameCn", CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang())?areaEntity.getNameCn():areaEntity.getNameEn());
        object.put("level", areaEntity.getLevel());
        object.put("benefitAmount", calculateBenefitAmount(areaEntity.getCode(), year, source)); //受益金额
        return object;
    }

    public JSONObject getFeeChildren(AreaEntity areaEntity, List<AreaEntity> areaEntities, List<CountryMarketingBudgetEntity> marketingBudgetEntities, String year) {
        JSONObject object = null;
        BigDecimal benefitAmount = BigDecimal.ZERO;
        if (GLOBAL_AREA_CODE.equals(areaEntity.getCode()) || areaEntity.getCode().contains("HQ")) {
            object = getJsonObject(areaEntity, year, "MBF/MSF");
            benefitAmount = getBenifit(year, areaEntity.getCode(), areaEntity.getLevel(), null);
        } else {
            object = getJsonObject(areaEntity, year, "MBF");
            benefitAmount = getBenifit(year, areaEntity.getCode(), areaEntity.getLevel(), "MBF");
        }
        BigDecimal calculate = calculate(marketingBudgetEntities, areaEntity.getCode(), null); //预算总金额
        object.put("amount", calculate);

        object.put("benefitAmount", benefitAmount);
        object.put("remainderAmount", BigDecimal.ZERO);
        object.put("quarterAmount", BigDecimal.ZERO);
        if (areaEntity.getLevel().equals("3")) {
            BigDecimal quarterAmount = getQuarterAmount(areaEntity.getCode(), year, areaEntity.getLevel());

            if (areaEntity.getCode().contains("HQ")) {
                // 此时计算季度累计预算金额剩余 = MBF的资金池 - 季度累计预算（只有MBF下拉选）
                calculate = calculate(marketingBudgetEntities, areaEntity.getCode(), "MBF"); //预算总金额
            }
            if (null != quarterAmount) {
                object.put("quarterAmount", quarterAmount);
                object.put("remainderAmount", calculate.subtract(quarterAmount));
            } else {
                object.put("quarterAmount", BigDecimal.ZERO);
                object.put("remainderAmount", calculate);
            }
            return object;
        } else {
            JSONArray array = new JSONArray();
            for (AreaEntity entity : areaEntities) {
                if (StringUtils.isNotBlank(entity.getParentId()) && entity.getParentId().equals(areaEntity.getCode())) {
                    array.add(getFeeChildren(entity, areaEntities, marketingBudgetEntities, year));
                }
            }
            object.put("children", array);
        }
        return object;
    }

    public BigDecimal getBenifit(String year, String code, String level, String source) {
        HashMap<String, String> map = new HashMap<>();
        map.put("year", year);
        map.put("source", source);
        if (level.equals("1")) {

        } else if (level.equals("2")) {
            map.put("areaCode", code);
        } else if (level.equals("3")) {
            map.put("representative", code);
        }
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
        return benefitAmount;
    }


    public BigDecimal calculateBenefitAmount(String code, String year, String source) {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("year", year);
        map.put("source", source);
        BigDecimal bigDecimal = marketingBudgetDao.calculateBenefitAmount(map);
        if (null == bigDecimal) {
            bigDecimal = BigDecimal.ZERO;
        }
        return bigDecimal;
    }


    public BigDecimal getQuarterAmount(String code, String year, String level) {
        BigDecimal quarterAmount = BigDecimal.ZERO;
        if (AreaLevelEnum.REPRESENT_OFFICE.getLevel().equals(level)) {
            BigDecimal amount = service.getQuarterAmount(code, year);
            if (null != amount) {
                quarterAmount = quarterAmount.add(amount);
            }

        } else {
            // 如果是上级地区部如HQ、拉美、亚太 // 找到所有子其代表处并累加季度授予预算
            Wrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
            areaWrapper.eq("parent_id", code);
            List<AreaEntity> childAreas = dao.selectList(areaWrapper);
            for (AreaEntity areaEntity: childAreas) {
                BigDecimal amount = service.getQuarterAmount(areaEntity.getCode(), year);
                if (null != amount) {
                    quarterAmount = quarterAmount.add(amount);
                }
            }
        }

        return quarterAmount;
    }


    public BigDecimal calculate(List<CountryMarketingBudgetEntity> marketingBudgetEntities, String code, String source) {
        BigDecimal sum = BigDecimal.ZERO;
        for (CountryMarketingBudgetEntity entity : marketingBudgetEntities) {
            if(ObjectUtil.isNotEmpty(source)){
                if (entity.getAreaCode().equals(code) && source.equals(entity.getSource())) {
                    sum = sum.add(entity.getAmount());
                }
            } else{
                if (entity.getAreaCode().equals(code)) {
                    sum = sum.add(entity.getAmount());
                }
            }

        }
        return sum;
    }

    public JSONObject getChildren(AreaEntity areaEntity, List<AreaEntity> areaEntities, String level) {
        JSONObject object = new JSONObject();
        object.put("id", areaEntity.getId());
        object.put("code", areaEntity.getCode());
        object.put("nameCn", areaEntity.getNameCn());
        object.put("level", areaEntity.getLevel());
        if (areaEntity.getLevel().equals(level)) {
            return object;
        } else {
            JSONArray array = new JSONArray();
            for (AreaEntity entity : areaEntities) {
                if (StringUtils.isNotBlank(entity.getParentId()) && entity.getParentId().equals(areaEntity.getCode())) {
                    array.add(getChildren(entity, areaEntities,level));
                }
            }
            object.put("children", array);
        }
        return object;
    }

    /**
     * 查询地区、代表处、国家代表处
     *
     * @return
     */
    @Override
    public List<AreaEntity> listWithTree() {
        // 1.查询parent_id不为空的
        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.isNotNull("parent_id");
        List<AreaEntity> entities = baseMapper.selectList(wrapper);
        // 2.添加HQ数据
        List<AreaEntity> list = new ArrayList<>();
        for (AreaEntity entity : entities) {
            if (null != entity.getLevel() && entity.getLevel().equals("4") && !entity.getParentId().equals("RO_HQ")) {
                AreaEntity areaEntity = new AreaEntity();
                areaEntity.setCode(entity.getCode());
                areaEntity.setParentId("RO_HQ");
                areaEntity.setNameCn(entity.getNameCn());
                areaEntity.setNameEn(entity.getNameEn());
                list.add(areaEntity);
            }
        }
        if (null !=list && list.size() !=0) {
            entities.addAll(list);
        }

        // 3. 组装成树形结构
        List<AreaEntity> level1Menus = entities.stream()
                // 3.1 找到所有的一级科目(特点: getParentId == 0)
                .filter(areaEntity -> "01".equals(areaEntity.getParentId()))
                // 3.2 查找所有一级科目的子科目
                .map((menu) -> {
                    menu.setChildren(getAreaEntitys(menu, entities));
                    return menu;
                    // 3.3 组成List数组
                }).collect(Collectors.toList());
        // 4.返回树形菜单
        return level1Menus;
    }

    /**
     * 查询地区、代表处、国家代表处
     *
     * @return
     */
    @Override
    public List<AreaEntity> listWithTree1(Long userId) {
        // 1.查询List (parent_id不为空以及level！=3)
        Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
        wrapper.isNotNull("parent_id");
        wrapper.notIn("level","3");
        List<AreaEntity> entities = baseMapper.selectList(wrapper);
        // 2.查询当前人所拥有的代表处地区权限
        List<AreaEntity> authAreaLists = baseMapper.selectAuthAreaById(userId);
        authAreaLists.forEach(areaEntity -> {
            entities.add(areaEntity);
        });
        // 3.添加HQ数据
        List<AreaEntity> list = new ArrayList<>();
        for (AreaEntity entity : entities) {
            if (null != entity.getLevel() && entity.getLevel().equals("4") && !entity.getParentId().equals("RO_HQ")) {
                AreaEntity areaEntity = new AreaEntity();
                areaEntity.setCode(entity.getCode());
                areaEntity.setParentId("RO_HQ");
                areaEntity.setNameCn(entity.getNameCn());
                areaEntity.setNameEn(entity.getNameEn());
                list.add(areaEntity);
            }
        }
        if (null !=list && list.size() !=0) {
            entities.addAll(list);
        }

        // 4. 组装成树形结构
        List<AreaEntity> level1Menus = entities.stream()
                // 4.1 找到所有的一级科目(特点: getParentId == 0)
                .filter(areaEntity -> "01".equals(areaEntity.getParentId()))
                // 4.2 查找所有一级科目的子科目
                .map((menu) -> {
                    menu.setChildren(getAreaEntitys(menu, entities));
                    return menu;
                    // 4.3 组成List数组
                }).collect(Collectors.toList());
        // 5.把符合条件的数据进行返回
        List<AreaEntity> resMenus = new ArrayList<>();
        level1Menus.forEach(areaEntity -> {
            if (areaEntity.getChildren().size() > 0) {
                resMenus.add(areaEntity);
            }
        });
        // 4.返回数据
        return resMenus;
    }

    @Override
    public HashSet<String> getAllCtyByUserId() {
        SysUserEntity userEntity = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
        Long userId = userEntity.getUserId();
        // 先获取用户对应的areacode
        EntityWrapper<SysUserArea> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        List<String> sysUserAreaCodes = sysUserAreaDao.selectList(wrapper).stream().map(SysUserArea::getAreaCode)
                .collect(Collectors.toList());

        //
        HashSet<String> allCtyByUserOfIds = new HashSet<>();
        sysUserAreaCodes.forEach((sysUserAreaCode) -> {
                    List<CArea> allCtyByUserIds = sysUserAreaDao.getAllCtyByUserId(sysUserAreaCode);
                    String[] split = allCtyByUserIds.get(0).getIdss().split(",");
                    allCtyByUserOfIds.addAll(Arrays.asList(split));
                }
        );


        // wrapper.eq("level", "4");
        EntityWrapper<AreaEntity> wrapper1 = new EntityWrapper<>();
        wrapper1.eq("level", "4");
        List<String> sysUserAreaCodeseq4 = dao.selectList(wrapper1).stream().map(AreaEntity::getCode)
                .collect(Collectors.toList());

        // 取交集
        Set<String> collect = sysUserAreaCodeseq4.stream().filter(allCtyByUserOfIds::contains).collect(Collectors.toSet());
        return (HashSet<String>) collect;
    }

    @Override
    public R updateAreaTree(AreaEntity areaEntity) {
        EntityWrapper<AreaEntity> wrapper1 = new EntityWrapper<>();
        wrapper1.eq("code",areaEntity.getCode());
        this.update(areaEntity,wrapper1);
        return R.ok();
    }

    @Override
    public R deleteAreaTree(AreaEntity areaEntity) {
        if ("4".equals(areaEntity.getLevel())) {
            dao.deleteById(areaEntity.getId());
        } else {
            dao.deleteTreeNode(areaEntity.getCode());

        }
        return R.ok();
    }

    @Override
    public R insertAreaTree(AreaEntity areaEntity) {
        if(!"4".equals(areaEntity.getLevel())){
            Wrapper<AreaEntity> wrapper = new EntityWrapper<>();
            wrapper.eq("code",areaEntity.getCode());
            int count = this.selectCount(wrapper);
            if (count > 0) {
                return R.error(100, "区域code已存在，请重新设置");
            }
        }

        List<Country> countries =new ArrayList<>(1);
        if("4".equals(areaEntity.getLevel())){
            Wrapper<Country> wrapper1 = new EntityWrapper<>();
            wrapper1.eq("CTY_CODE",areaEntity.getCode());
            countries=countryMapper.selectList(wrapper1);
            areaEntity.setNameCn(countries.get(0).getNameCn());
            areaEntity.setNameEn(countries.get(0).getNameEn());
        }
        else{
            areaEntity.setNameCn(areaEntity.getNameCn());
            areaEntity.setNameEn(areaEntity.getNameEn());
        }

        this.insert(areaEntity);
        return R.ok();
    }

    @Override
    public List<Country> queryCountryList() {
        return  countryMapper.selectList(null);
    }

    /**
     * 获取地区子类
     *
     * @param menu
     * @param entities
     * @return
     */
    private List<AreaEntity> getAreaEntitys(AreaEntity menu, List<AreaEntity> entities) {
        List<AreaEntity> children = entities.stream()
                .filter((areaEntity) -> areaEntity.getParentId().equals(menu.getCode()))
                .map(areaEntity -> {
                    areaEntity.setChildren(getAreaEntitys(areaEntity, entities));
                    return areaEntity;
                }).collect(Collectors.toList());
        return children;

    }



}
