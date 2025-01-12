package com.wiko.emarket.controller.foreign;

import com.alibaba.fastjson.JSONObject;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.service.acceptance.ReceiptInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 发票凭证
 *
 * @Author shaofeng Guo
 * @Date 2022/6/30 15:54
 * @description: TODO
 **/
@RestController
@RequestMapping("/foreign/v1/receipt")
@Slf4j
public class ReceiptController extends AbstractController {
    @Resource
    private ReceiptInfoService receiptInfoService;

    /**
     * 保存发票凭证信息
     *
     * @return
     */
    @RequestMapping("/saveReceiptInfo")
    public JSONObject saveReceiptInfo(@RequestBody Map<String,Object> map){
        return receiptInfoService.saveReceiptInfo(map);
    }
}

