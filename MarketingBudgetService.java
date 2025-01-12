package com.wiko.emarket.service.campaign;

import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.MarketingBudgetVO;
import com.wiko.emarket.vo.marketingBudget.ShowBudgetVO;

import java.util.List;
import java.util.Map;

/**
 * @author WIKO
 * @Date: 2022/4/27 - 04 - 27 - 15:58
 * @projectName:PSI
 * @Description: com.wiko.emarket.service
 */
public interface MarketingBudgetService {
    /**
     * 查询全球和地区部一级资源池数据
     *
     * @param params 查询数据
     * @return
     */
    List<MarketingBudgetVO> queryMarketingBudget(Map<String, Object> params);

    /**
     * 更新营销预算表数据，预计删除
     *
     * @param marketingBudgetVOs 查询数据
     * @return
     */
    Map<String, Object> deleteMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs);
    /**
     * 保存和更新营销预算表地区数据
     *
     * @param marketingBudgetVOs 查询数据
     * @return
     */
    Map<String,Object> saveMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs,String lang);
    /**
     * 保存和更新营销预算表地区数据---代表处预算金额保存， 才会走该接口
     *
     * @param marketingBudgetVOs 查询数据
     * @return
     */
    Map<String,Object> saveMarketingBudgetLv1(List<MarketingBudgetVO> marketingBudgetVOs,String lang);
    /**
     * 查询全球预算与已启动收益金额
     *
     * @param year 查询数据
     * @return
     */
    List<ShowBudgetVO> queryHqBudget(String year);
    /**
     * 查询地区部预算与已启动收益金额
     *
     * @param map 查询数据
     * @return
     */
    List<ShowBudgetVO> queryAreaBudget(Map<String, String> map);
    /**
     * 查询预算类型
     *
     * @param map 查询数据
     * @return
     */
    List<BudgetTypeEntity> queryBudgetType(Map<String, String> map);

}
