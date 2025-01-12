package com.wiko.emarket.service.campaign.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.service.SysDictService;
import com.wiko.emarket.constant.ErrorCode;
import com.wiko.emarket.constant.PrPoStatusEnum;
import com.wiko.emarket.dao.CompanyDao;
import com.wiko.emarket.service.campaign.SapRfcService;
import com.wiko.emarket.vo.PrNumberRequestVO;
import com.wiko.emarket.vo.SapCompanyVO;
import com.wiko.emarket.vo.SapCostCenterVO;
import com.wiko.psi.sap.RfcService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @ClassName SapRfcServiceImpl
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/6/29 9:28
 * @Version 1.0
 **/
@Service
@Slf4j
public class SapRfcServiceImpl implements SapRfcService {
    @Autowired
    RfcService rfcService;
    @Autowired
    private CompanyDao companyDao;

    @Autowired
    private SysDictService sysDictService;

    @Transactional(rollbackFor = Exception.class)
    public void getCompanyList() {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "FI0001");
        params.put("IV_USER", "marketing");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("PNAME", "KTOPL");
        jsonObject.put("VALUE", "1000");
        JSONArray array = new JSONArray();
        array.add(jsonObject);
        params.put("IV_INPUT", array.toJSONString());
        List<String> inTable = new ArrayList<String>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<List<Map<String, Object>>>();
        List<String> outTable = new ArrayList<String>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<List<Map<String, Object>>>();

        //查询参数1: 采购订单编号

        List<Map<String, Object>> inMaps = null;
        Map<String, Object> inMap = null;
        inMaps = new ArrayList<Map<String, Object>>();
        inMap = new HashMap<String, Object>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        List<SapCompanyVO> companyList = new ArrayList<SapCompanyVO>();
        //调用RFC
        try {
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.warn("getCompanyList get sap info" + execute);
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());
            JSONObject value = (JSONObject) jsonArray.get(0);
            JSONArray jsonArray1 = JSONArray.parseArray(value.get("VALUE").toString());
            companyList = jsonArray1.toJavaList(SapCompanyVO.class);
        } catch (Throwable e) {
            log.error("getCompanyList fail" + params, e);
        }
        companyDao.saveCompanyInfo(companyList);
    }

    @Transactional(rollbackFor = Exception.class)
    public void getCostCenterData() {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "FI0002");
        params.put("IV_USER", "marketing");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("PNAME", "KOKRS");
        jsonObject.put("VALUE", "COCN");
        JSONArray array = new JSONArray();
        array.add(jsonObject);
        params.put("IV_INPUT", array.toJSONString());
        List<String> inTable = new ArrayList<String>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<List<Map<String, Object>>>();
        List<String> outTable = new ArrayList<String>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<List<Map<String, Object>>>();

        List<Map<String, Object>> inMaps = null;
        Map<String, Object> inMap = null;
        inMaps = new ArrayList<Map<String, Object>>();
        inMap = new HashMap<String, Object>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        List<SapCostCenterVO> costCenterList = new ArrayList<SapCostCenterVO>();
        //调用RFC
        try {
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.warn("getCostCenterData get sap info" + execute);
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());
            JSONObject prObject = JSONObject.parseObject(jsonArray.get(0).toString());
            JSONArray value = JSONArray.parseArray(prObject.getString("VALUE"));
            costCenterList = value.toJavaList(SapCostCenterVO.class);
        } catch (Throwable e) {
            log.error("getCostCenterData fail" + params, e);
        }

        companyDao.saveCostCenterInfo(costCenterList);
    }

    @Override
    public Map<String, Object> getPoNumber(PrNumberRequestVO vo) {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "MM0004");
        params.put("IV_USER", vo.getUserName());
        // 构建IV_INPUT参数
        JSONArray arrays = buildInputParams(vo);
        log.info("SapRfcServiceImpl -> getPrNumber params {}", arrays.toJSONString());
        params.put("IV_INPUT", arrays.toJSONString());
        List<String> inTable = new ArrayList<String>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<List<Map<String, Object>>>();
        List<String> outTable = new ArrayList<String>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<List<Map<String, Object>>>();

        List<Map<String, Object>> inMaps = null;
        Map<String, Object> inMap = null;
        inMaps = new ArrayList<>();
        inMap = new HashMap<>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        Map<String, Object> outMap = new HashMap<>();
        log.info("query sap pr param ={}" , params);

        //调用RFC
        try {
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.info("getPrNumber get sap info" + execute);
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());
            JSONObject jsonObject2 = JSONObject.parseObject(jsonArray.get(0).toString());
            JSONObject msg = JSONObject.parseObject(jsonObject2.getString("VALUE"));
            if ("E".equals(msg.getString("MSGTY"))) {
                log.error("getPrNumber error, msg:{}", msg);
                outMap.put("errorCode", ErrorCode.OUTPUT_ERROR);
                outMap.put("msg", msg.getString("MSGTX"));
                return outMap;
            }
            JSONObject prObject = JSONObject.parseObject(jsonArray.get(1).toString());
            outMap.put("errorCode", ErrorCode.SUCCESS);
            outMap.put("prNo", prObject.getString("VALUE").replace("\"", ""));
        } catch (Throwable e) {
            log.error("query sap getPrNumber fail ={}", params, e);
            outMap.put("errorCode", ErrorCode.ERROR);
        }
        return outMap;
    }

    private JSONArray buildInputParams(PrNumberRequestVO vo) {
        // 1.判断是自行采购还是一般采购
        String type = "purchaseType";
        String code = "1";
        SysDictEntity sysDist = sysDictService.getSysDist(type, code);
        JSONArray arrays = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        if (sysDist.getValue().equals(vo.getPurchaseType())) {
            // 1.1自行采购
            jsonObject.put("PNAME", "I_BSART");
            jsonObject.put("VALUE", "ZSP");
        }else {
            // 1.2一般采购
            jsonObject.put("PNAME", "I_BSART");
            jsonObject.put("VALUE", "FYNB");
        }
        arrays.add(jsonObject);
        // 2.加上pr字段的值
        JSONObject prJsonObject = new JSONObject();
        prJsonObject.put("PNAME", "I_NUMBER");
        prJsonObject.put("VALUE", vo.getPrId());
        arrays.add(prJsonObject);

        JSONObject jsonObjects = new JSONObject();
        jsonObjects.put("KNTTP", "Z");
        jsonObjects.put("MAKTX", vo.getActivityDescription());
        jsonObjects.put("WGBEZ", vo.getActivityName());
        jsonObjects.put("MENGE", vo.getQty());
        jsonObjects.put("EEIND", vo.getEndDate());
        jsonObjects.put("MATKL", vo.getActivityTypeCode());
        jsonObjects.put("WERKS", vo.getCompanyCode());
        jsonObjects.put("KOSTL", vo.getCostCenterCode());
        jsonObjects.put("BEDNR", vo.getActivityId());
        jsonObjects.put("PREIS", vo.getAmount());
        jsonObjects.put("AFNAM", vo.getUserName());
        jsonObjects.put("WAERS", vo.getCurrency());
        // sap长度单位60+40+40+40+40需要进行截断
        truncateAddress(jsonObjects, vo.getAddress());
        JSONArray array1 = new JSONArray();
        array1.add(jsonObjects);
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("PNAME", "T_PR");
        jsonObject1.put("VALUE", array1.toJSONString());
        arrays.add(jsonObject1);
        return arrays;
    }

    @Override
    public Map<String, Object> getOrderData(JSONObject orderJson) {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "MM0005");
        params.put("IV_USER", "marketing");
        // 构建IV_INPUT参数
        JSONArray array1 = new JSONArray();
        array1.add(orderJson);
        JSONObject jsonObjects = new JSONObject();
        jsonObjects.put("PNAME", "EKPO");
        jsonObjects.put("VALUE", array1.toJSONString());
        JSONArray arrays = new JSONArray();
        arrays.add(jsonObjects);
        params.put("IV_INPUT", arrays.toJSONString());
        List<String> inTable = new ArrayList<String>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<List<Map<String, Object>>>();
        List<String> outTable = new ArrayList<String>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<List<Map<String, Object>>>();

        List<Map<String, Object>> inMaps = null;
        Map<String, Object> inMap = null;
        inMaps = new ArrayList<Map<String, Object>>();
        inMap = new HashMap<String, Object>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        Map<String, Object> outMap = new HashMap<>();
        //调用RFC
        try {
            log.info("getOrderData get sap info param:{}", params);
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.info("getOrderData get sap info response:{}", execute);
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());
            // 取返回码
            JSONObject jsonObject2 = JSONObject.parseObject(jsonArray.get(0).toString());
            JSONObject msg = JSONObject.parseObject(jsonObject2.getString("VALUE"));
            if ("E".equals(msg.getString("MSGTY"))) {
                outMap.put("errorCode", ErrorCode.OUTPUT_ERROR);
                outMap.put("msg", msg.getString("MSGTX"));
                return outMap;
            }
            JSONObject jsonObject = JSONObject.parseObject(jsonArray.get(1).toString());
            outMap.put(jsonObject.getString("PNAME"), jsonObject.getString("VALUE").replace("\"", ""));
            JSONObject jsonObject1 = JSONObject.parseObject(jsonArray.get(2).toString());
            outMap.put(jsonObject1.getString("PNAME"), jsonObject1.getString("VALUE").replace("\"", ""));
            outMap.put("errorCode", 0);

        } catch (Throwable e) {
            log.error("query sap OrderData fail" + params, e);
            outMap.put("errorCode", ErrorCode.ERROR);
        }
        return outMap;
    }

    private void truncateAddress(JSONObject jsonObjects, String address) {
        if (StringUtils.isEmpty(address)) {
            return;
        }
        if (address.length() <= 60) {
            jsonObjects.put("STREET", address);
            return;
        }
        if (60 <= address.length() && address.length() <= 100) {
            jsonObjects.put("STREET", address.substring(0, 59));
            jsonObjects.put("STR_SUPPL1", address.substring(60));
            return;

        }
        if (100 <= address.length() && address.length() <= 140) {
            jsonObjects.put("STREET", address.substring(0, 59));
            jsonObjects.put("STR_SUPPL1", address.substring(60, 99));
            jsonObjects.put("STR_SUPPL2", address.substring(100));
            return;

        }
        if (140 <= address.length() && address.length() <= 180) {
            jsonObjects.put("STREET", address.substring(0, 59));
            jsonObjects.put("STR_SUPPL1", address.substring(60, 99));
            jsonObjects.put("STR_SUPPL2", address.substring(100, 139));
            jsonObjects.put("STR_SUPPL3", address.substring(140));
            return;
        }
        if (180 <= address.length()) {
            jsonObjects.put("STREET", address.substring(0, 59));
            jsonObjects.put("STR_SUPPL1", address.substring(60, 99));
            jsonObjects.put("STR_SUPPL2", address.substring(100, 139));
            jsonObjects.put("STR_SUPPL3", address.substring(140, 179));
            if (address.length() <= 220) {
                jsonObjects.put("LOCATION", address.substring(140));
                return;
            } else {
                jsonObjects.put("LOCATION", address.substring(180, 219));
                return;
            }
        }
    }

    @Override
    public Map<String, Object> getPoStatus(JSONArray prArray) {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "MM0007");
        params.put("IV_USER", "marketing");

        // 构建IV_INPUT参数
        JSONArray rEblenArray = new JSONArray();
        for (Object poId: prArray) {
            JSONObject jsonObjects = new JSONObject();
            jsonObjects.put("SIGN", "I");
            jsonObjects.put("OPTION", "EQ");
            jsonObjects.put("LOW", poId.toString());
            rEblenArray.add(jsonObjects);
        }

        JSONObject rEblenJson = new JSONObject();
        rEblenJson.put("PNAME", "R_EBELN");
        rEblenJson.put("VALUE", rEblenArray.toJSONString());

        JSONArray inputsArray = new JSONArray();
        inputsArray.add(rEblenJson);
        params.put("IV_INPUT", inputsArray.toJSONString());

        List<String> inTable = new ArrayList<>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<>();
        List<String> outTable = new ArrayList<>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<>();

        List<Map<String, Object>> inMaps = new ArrayList<>();
        Map<String, Object> inMap = new HashMap<>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        Map<String, Object> outMap = new HashMap<>();
        //调用RFC
        try {
            long time = System.currentTimeMillis();
            log.info("getPrStatus from sap param:{}", params);
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.info("getPoStatus from sap response:{}, took time:{}", execute, (System.currentTimeMillis() - time));
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());

            // 此接口SAP没有返回码， 只能取数据， 判断数据是否有值
            // 解析数组
            for (Object obj: jsonArray){
                JSONObject jsonObject = JSONObject.parseObject(obj.toString());
                if ("ZEKPO".equalsIgnoreCase(jsonObject.getString("PNAME"))) {
                    outMap.put(jsonObject.getString("PNAME"), jsonObject.getString("VALUE").replace("\\", ""));
                }
            }
            outMap.put("errorCode", 0);
        } catch (Throwable e) {
            log.error("getPoStatus from sap fail, param:{}", params, e);
            outMap.put("errorCode", ErrorCode.ERROR);
        }
        return outMap;
    }

    @Override
    public Map<String, Object> getPrStatus(JSONArray prArray) {
        String rfcName = "ZFM_IF_COM";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IV_IFNUM", "MM0010");
        params.put("IV_USER", "marketing");

        // 构建IV_INPUT参数
        JSONArray rEblenArray = new JSONArray();
        for (Object poId: prArray) {
            JSONObject jsonObjects = new JSONObject();
            jsonObjects.put("SIGN", "I");
            jsonObjects.put("OPTION", "EQ");
            jsonObjects.put("LOW", poId.toString());
            rEblenArray.add(jsonObjects);
        }

        JSONObject rEblenJson = new JSONObject();
        rEblenJson.put("PNAME", "TR_BANFN");
        rEblenJson.put("VALUE", rEblenArray.toJSONString());

        JSONArray inputsArray = new JSONArray();
        inputsArray.add(rEblenJson);
        params.put("IV_INPUT", inputsArray.toJSONString());

        List<String> inTable = new ArrayList<>();
        List<List<Map<String, Object>>> inRecords = new ArrayList<>();
        List<String> outTable = new ArrayList<>();
        List<List<Map<String, Object>>> outRecords = new ArrayList<>();

        List<Map<String, Object>> inMaps = new ArrayList<>();
        Map<String, Object> inMap = new HashMap<>();
        inMaps.add(inMap);
        inRecords.add(inMaps);
        Map<String, Object> outMap = new HashMap<>();
        //调用RFC
        try {
            long time = System.currentTimeMillis();
            log.info("getPrStatus from sap param:{}", params);
            Map<String, Object> execute = rfcService.execute(rfcName, params, inTable, inRecords, outTable, outRecords);
            log.info("getPrStatus from sap response:{}, took time:{}", execute, (System.currentTimeMillis() - time));
            JSONArray jsonArray = JSONArray.parseArray(execute.get("EV_OUTPUT").toString());

            // 此接口SAP没有返回码， 只能取数据， 判断数据是否有值
            // 解析数组
            for (Object obj: jsonArray){
                JSONObject jsonObject = JSONObject.parseObject(obj.toString());
                if ("T_PR_LST".equalsIgnoreCase(jsonObject.getString("PNAME"))) {
                    outMap.put(jsonObject.getString("PNAME"), jsonObject.getString("VALUE").replace("\\", ""));
                }
            }
            outMap.put("errorCode", 0);
        } catch (Throwable e) {
            log.error("getPrStatus from sap fail, param:{}", params, e);
            outMap.put("errorCode", ErrorCode.ERROR);
        }
        return outMap;
    }

    /**
     * PR状态码查询
     *
     * @param prId
     * @return
     */
    @Override
    public String getPrStatusCode(String prId) {
        // 调SAP系统接口获取PO状态
        JSONArray prArray = new JSONArray();
        prArray.add(prId);
        Map<String, Object> resMap = getPrStatus(prArray);

        if (null == resMap.get("T_PR_LST") || resMap.get("T_PR_LST").toString().isEmpty()) {
            return null;
        }
        String value = resMap.get("T_PR_LST").toString();
        JSONArray values = JSONArray.parseArray(value);

        for(Object obj : values){
            JSONObject jsonObj = JSONObject.parseObject(obj.toString()) ;
            if (!Objects.equals(jsonObj.getString("EBAKZ"), "X")) {
                // 未关闭
                return PrPoStatusEnum.NORMAL.getCode();
            }
        }

        // 已关闭
        return PrPoStatusEnum.CLOSED.getCode();
    }

    public static void main(String[] args) {
        String a = "222sd";
        String b = "222sd";
        String c = new String("222sd");
//        System.out.println(a == b);
        System.out.println(a == c);
    }
}
