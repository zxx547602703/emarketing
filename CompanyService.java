package com.wiko.emarket.service.campaign;

import com.wiko.emarket.vo.CompanyVO;
import com.wiko.emarket.vo.CostCenterVO;

import java.util.List;
import java.util.Map;

/**
 * @ClassName CompanyService
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 17:14
 * @Version 1.0
 **/

public interface CompanyService {
    /**
     * 查询公司清单
     *
     * @return list 查询数据
     */
    List<CompanyVO> queryComPanyList();
    /**
     * 查询成本中心清单，传递公司code则为根据公司查询
     *
     * @param params
     * @return list 查询数据
     */
    List<CostCenterVO> queryCostCenter(Map<String,Object> params);


}
