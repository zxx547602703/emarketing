package com.wiko.emarket.controller.foreign;

import com.alibaba.fastjson.JSONObject;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.service.acceptance.PaymentVoucherInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Author shaofeng Guo
 * @Date 2022/6/30 15:54
 * @description: TODO
 **/
@RestController
@RequestMapping("/foreign/v1/paymentVoucherInfo")
@Slf4j
public class PaymentVoucherInfoController extends AbstractController {
    @Resource
    private PaymentVoucherInfoService paymentVoucherInfoService;


    /**
     * 查询第一验收人
     * @return
     */
    @RequestMapping("/savePaymentVoucherInfo")
    public JSONObject savePaymentVoucherInfo(@RequestBody Map<String,Object> map){
        return paymentVoucherInfoService.savePaymentVoucherInfo(map);
    }
}

