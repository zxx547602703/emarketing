package com.wiko.emarket.service.campaign.impl;

import com.wiko.emarket.dao.ExchangeDao;
import com.wiko.emarket.entity.Exchange;
import com.wiko.emarket.service.campaign.ExchangeService;
import com.wiko.emarket.vo.ExchangeRateInfoVo;
import com.wiko.psi.sap.RfcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Auther: brando
 * @Date: 2022/4/29 - 04 - 29 - 16:47
 * @projectName:PSI
 * @Description: com.wiko.psi.service.impl
 */
@Service
@Slf4j
@Component("exchangeService")
public class ExchangeServiceImpl implements ExchangeService {
    @Autowired
    RfcService rfcService;
    @Autowired
    private ExchangeDao exchangeDao;

    public void saveExchange() {
        List<Exchange> exchangeList = new ArrayList<>();
        List<String> list = exchangeDao.queryCurrency();
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd");
        String date = simpleFormat.format(new Date());
        for (String currency : list
        ) {
            String rfcName = "ZGL_GET_STANDARD_MONEY";
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("BUKRS", "");
            params.put("WAERK", currency);
            params.put("WAERK_TO", "USD");
            params.put("DATE", date);
            List<String> inTable = new ArrayList<String>();
            List<List<Map<String, Object>>> inRecords = new ArrayList<List<Map<String, Object>>>();
            List<String> outTable = new ArrayList<String>();
            List<List<Map<String, Object>>> outRecords = new ArrayList<List<Map<String, Object>>>();
            //调用RFC
            try {
                Map<String, Object> resultMap = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
                if(resultMap.isEmpty()|| resultMap.get("HWAER")==null || resultMap.get("KURSF")==null){
                    log.error("query sap exchange is null" + params);
                    continue;
                }
                Exchange exchange = new Exchange();
                exchange.setFromCurrency(currency);
                exchange.setRateDate(date.substring(0, date.length() - 2));
                exchange.setToCurrency((String) resultMap.get("HWAER"));
                BigDecimal oppositeRate = (BigDecimal) resultMap.get("KURSF");
                if(oppositeRate.compareTo(BigDecimal.ZERO)==0){
                    log.error("query sap exchange is zero" + params);
                    continue;
                }
                // sap查询汇率接口的不存在汇率时会返回0 ，查询结果（to_currency与from_currency相反)的时候是返回负数，需要反算
                if(oppositeRate.compareTo(BigDecimal.ZERO)==-1){
                    BigDecimal decimal = new BigDecimal(1);
                    exchange.setRate(decimal.divide(oppositeRate.abs(),7,BigDecimal.ROUND_HALF_UP));
                }
                else{
                    exchange.setRate(oppositeRate);
                }

                exchangeList.add(exchange);
            } catch (Throwable e) {
                log.error("query sap exchange fail" + params, e);
            }
        }
        exchangeDao.saveExchange(exchangeList);
    }


    @Override
    public BigDecimal queryExchange(Map<String, Object> param) {
        BigDecimal decimal = exchangeDao.queryExchange(param);
        // 如果传递月份汇率查询不到，直接使用最新的的汇率
        if(decimal==null){
            param.put("rateDate",null);
            decimal=  exchangeDao.queryExchange(param);
        }
        return decimal;
    }

    @Override
    public List<ExchangeRateInfoVo> queryAllExchange() {
        return exchangeDao.selectList(null);
    }

    @Override
    public List<ExchangeRateInfoVo> getLastestRateList() {
        return exchangeDao.getLastestRateList();
    }
}
                          