package com.wiko.emarket.dao;

import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.MarketingBudgetVO;
import com.wiko.emarket.vo.marketingBudget.AmountVO;
import com.wiko.emarket.vo.marketingBudget.AreaCountryVO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author WIKO
 * @Date: 2022/4/27 - 04 - 27 - 15:16
 * @projectName:PSI
 * @Description: com.wiko.emarket.dao
 */
@Mapper
public interface BudgetDao {
    /**
     * 查询营销预算表数据
     *
     * @param params 查询数据
     * @return
     */
    List<MarketingBudgetVO> queryMarketingBudget(Map<String, Object> params);


    /**
     * 查询其余地区部营销预算表数据
     *
     * @param params 查询数据
     * @return
     */
    List<MarketingBudgetVO> queryOtherMarketingBudget(Map<String, Object> params);

    /**
     * 更新营销预算表数据
     *
     * @param marketingBudgetVOs 查询数据
     * @return
     */
    void updateMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs);

    /**
     * 保存营销预算表数据
     *
     * @param marketingBudgetVOs 查询数据
     */
    void saveMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs);

    void deleteMarketingBudget(List<MarketingBudgetVO> marketingBudgetVOs);

    List<String> findSubBudget(Map<String, Object> budgetMap);

    /**
     * 根据parentId查询父级预算
     *
     * @param parentId 查询数据
     * @return
     */
    BigDecimal queryMarketingBudgetAmount(String parentId);

    /**
     * 查询全球预算金额和uuid
     *
     * @param map 查询数据
     * @return
     */
    MarketingBudgetVO queryAmountUUid(Map<String,String> map);

    /**
     * 查询收益金额和汇率
     *
     * @param map 查询数据
     * @return
     */
    List<AmountVO> queryAmountRate (Map<String,String> map);

    /**
     * 查询国家地区已收益金额
     *
     * @param map
     * @return
     */
    List<AmountVO> queryAmountRateV2 (Map<String,Object> map);

    /**
     * 查询收益金额和汇率
     *
     * @param map 查询数据
     * @return
     */
    BigDecimal queryHqBudget (Map<String,String> map);

    /**
     * 查询国家地区对应信息
     *
     * @param map 查询数据
     * @return
     */
    List<AreaCountryVO> queryCountryArea(Map<String, String> map);

    /**
     * 查询预算类型
     *
     * @param map 查询数据
     * @return
     */
    List<BudgetTypeEntity> queryBudgetType(Map<String, String> map);

    /**
     * 查询下级预算总和
     *
     * @param map 查询数据
     * @return
     */
    BigDecimal queryLowerBudget(Map<String, String> map);
}
