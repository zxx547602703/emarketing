package com.wiko.emarket.controller;

import com.framework.common.annotation.SysLog;
import com.framework.common.utils.R;
import com.wiko.emarket.service.campaign.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author WIKO
 * @Date: 2022/4/29 - 04 - 29 - 16:47
 * @projectName:PSI
 * @Description: com.wiko.psi.service.impl
 */
@RestController
@RequestMapping("/api/exchange")
public class ExchangeServiceController  {
    @Autowired
    private ExchangeService exchangeService;
    /**
     * 修改用户
     */
    @SysLog("查询汇率")
    @RequestMapping("/queryExchange")
    public R queryExchange(@RequestParam Map<String, Object> param) {
        BigDecimal bigDecimal = exchangeService.queryExchange(param);
        return R.ok().put("data",bigDecimal);
    }

    @RequestMapping("/getLastestRates")
    public R queryExchange() {
        return R.ok().put("data",exchangeService.getLastestRateList());
    }
}
