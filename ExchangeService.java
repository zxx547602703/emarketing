package com.wiko.emarket.service.campaign;

import com.wiko.emarket.vo.ExchangeRateInfoVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author WIKO
 * @Date: 2022/4/29 - 04 - 29 - 16:45
 * @projectName:PSI
 * @Description: com.wiko.psi.service
 */
public interface ExchangeService {
    /**
     * 查询汇率信息
     *
     * @param param 查询数据
     */
    BigDecimal queryExchange(Map<String,Object> param);

    /**
     * 查询all汇率
     *
     * @return List<ExchangeRateInfoVo>
     */
    List<ExchangeRateInfoVo> queryAllExchange();

    /**
     * 获取最新的币种汇率列表
     * @return
     */
    List<ExchangeRateInfoVo> getLastestRateList();

}
