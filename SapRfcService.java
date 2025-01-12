package com.wiko.emarket.service.campaign;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wiko.emarket.vo.PrNumberRequestVO;

import java.util.Map;

/**
 * @ClassName SapRfcService
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/6/29 9:36
 * @Version 1.0
 **/
public interface SapRfcService {
    /**
     * 建立费用性采购申请参数
     *
     * @param vo
     * @return
     */
    Map<String, Object> getPoNumber(PrNumberRequestVO vo);

    /**
     * 采购订单收货参数
     *
     * @param orderJson
     * @return
     */
    Map<String, Object> getOrderData(JSONObject orderJson);


    /**
     * 获取PO状态
     *
     * @param prArray
     * @return
     */
    Map<String, Object> getPoStatus(JSONArray prArray);

    /**
     * 获取PR状态
     *
     * @param prArray
     * @return
     */
    Map<String, Object> getPrStatus(JSONArray prArray);

    /**
     * 获取PR状态码
     *
     * @param prId
     * @return
     */
    String getPrStatusCode(String prId);
}
