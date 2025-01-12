package com.wiko.emarket.service.campaign;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.service.IService;
import com.framework.common.utils.R;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.psi.entity.Country;

import java.util.HashSet;
import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:51
 */
public interface AreaService extends IService<AreaEntity> {
    JSONObject getAreaTree(String code,String level);

    JSONObject getUserAreaTree(Long userId);

    JSONArray getAreaTreeForSubmiter(Long userId);

    JSONObject getBudgetOrg(String rgCode);

    JSONObject getBudgetRO(String rgCode, String submitterRo);

    List<AreaEntity> getBudgetCountry(String roCode);

     List<BudgetTypeEntity> getBugetType(String roCode, String submiterRoCode);

    List<String> getBugetYear();

    JSONArray getAreaFee(String year, String code);

    List<AreaEntity> getAllArea();

    List<AreaEntity> getAllAreaCode(String code);

    JSONArray getAllAreaFeeTree(String year, Long userId);

    List<AreaEntity>  getAllAreaList(Long userId, String level);

    JSONArray getSingleAreaFee(String code, String year);

    List<AreaEntity> listWithTree();

    List<AreaEntity> listWithTree1(Long userId);

    HashSet<String> getAllCtyByUserId();

    R updateAreaTree(AreaEntity areaEntity);

    R deleteAreaTree(AreaEntity areaEntity);

    R insertAreaTree(AreaEntity areaEntity);

    List<Country> queryCountryList();

}
