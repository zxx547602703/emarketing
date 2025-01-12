package com.wiko.emarket.dao;

import com.wiko.emarket.vo.CompanyVO;
import com.wiko.emarket.vo.CostCenterVO;
import com.wiko.emarket.vo.SapCompanyVO;
import com.wiko.emarket.vo.SapCostCenterVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @ClassName CompanyDao
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/1 14:17
 * @Version 1.0
 **/
@Mapper
public interface CompanyDao {
    /**
     * 保存公司信息
     *
     * @param list 查询数据
     */
    void saveCompanyInfo(List<SapCompanyVO> list);
    /**
     * 保存成本中心数据
     *
     * @param list 查询数据
     */
    void saveCostCenterInfo(List<SapCostCenterVO> list);
    /**
     * 查询公司清单
     *
     * @return list 查询数据
     */
    List<CompanyVO> queryComPanyList();
    /**
     * 查询成本中心清单，传递公司code则为根据公司查询
     *
     * @return list 查询数据
     */
    List<CostCenterVO> queryCostCenter(Map<String,Object> params);

    CostCenterVO queryCostCenterInfo(Map<String,Object> params);
}
