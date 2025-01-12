package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.dao.BudgetTypeDao;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.service.campaign.BudgetTypeService;
import com.wiko.emarket.util.RequestUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/27 15:51
 */
@Service
public class BudgetTypeServiceImpl extends ServiceImpl<BudgetTypeDao, BudgetTypeEntity> implements BudgetTypeService {
    @Resource
    private BudgetTypeService budgetTypeService;
    @Override
    public List<BudgetTypeEntity> getBudgetType(String source, String areaCode) {
        List<BudgetTypeEntity> budgetTypeEntities = null;
        if(StringUtils.isEmpty(areaCode) || areaCode.contains("HQ")){
            budgetTypeEntities = budgetTypeService.selectList(null);
        }else {
            EntityWrapper<BudgetTypeEntity> entityEntityWrapper = new EntityWrapper<>();
            entityEntityWrapper.eq("source","MBF");
            budgetTypeEntities = budgetTypeService.selectList(entityEntityWrapper);
        }
        boolean isChinese= CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        if (!isChinese) {
            for (BudgetTypeEntity budgetType : budgetTypeEntities) {
                budgetType.setNameCn(budgetType.getNameEn());
            }
        }
        if(areaCode.equals("RG_HQ")){
            List<BudgetTypeEntity> ret = new ArrayList<>();
            if(source.equals("MBF")){
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(("BM").equals(entity.getCode()) || entity.getCode().equals("CM")){
                        ret.add(entity);
                    }
                }
                return ret;
            }else{
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(("RM").equals(entity.getCode()) || entity.getCode().equals("SM")){
                        ret.add(entity);
                    }
                }
            }
        } else if(areaCode.equals("RO_HQ_1") ){
            List<BudgetTypeEntity> ret = new ArrayList<>();
            if(source.equals("MBF")){
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(("BM").equals(entity.getCode())){
                        ret.add(entity);
                    }
                }
                return ret;
            }else if("MSF".equals(source)){
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(entity.getCode().equals("CM")){
                        ret.add(entity);
                    }
                }
            } else{
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(entity.getCode().equals("CM") || ("BM").equals(entity.getCode())){
                        ret.add(entity);
                    }
                }
            }
            return ret;
        } else if(areaCode.equals("RO_HQ_2")){
            List<BudgetTypeEntity> ret = new ArrayList<>();
            if(source.equals("MBF")){
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(("RM").equals(entity.getCode())){
                        ret.add(entity);
                    }
                }
                return ret;
            }else if(source.equals("MSF")){
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(entity.getCode().equals("SM")){
                        ret.add(entity);
                    }
                }
            }else {
                for(BudgetTypeEntity entity:budgetTypeEntities){
                    if(entity.getCode().equals("SM") || "RM".equals(entity.getCode())){
                        ret.add(entity);
                    }
                }
            }
            return ret;
        }else {
            return budgetTypeEntities;
        }
        return budgetTypeEntities;
    }
}
