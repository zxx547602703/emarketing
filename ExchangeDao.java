package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.Exchange;
import com.wiko.emarket.vo.ExchangeRateInfoVo;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author WIKO
 * @Date: 2022/4/29 - 04 - 29 - 16:41
 * @projectName:PSI
 * @Description: com.wiko.psi.dao
 */
@Mapper
public interface ExchangeDao extends BaseMapper<ExchangeRateInfoVo> {
    /**
     * 保存汇率信息
     *
     * @param exchangeList 查询数据
     */
    void saveExchange(List<Exchange> exchangeList);
    /**
     * 查询汇率信息
     *
     * @param param 查询数据
     * @return
     */
    BigDecimal queryExchange(Map<String, Object> param);

     /**
      * @Description //TODO查询币种
      * @Param []
      * @return java.util.List<java.lang.String>
      **/
    List<String> queryCurrency();

    /**
     * 获取最新的币种汇率列表
     * @return
     */
    List<ExchangeRateInfoVo> getLastestRateList();
}
