package com.wiko.emarket.dao;

import com.wiko.emarket.entity.FormPoLineInfo;
import com.wiko.emarket.entity.PoLineInfo;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.accceptance.AcceptanceDetailVO;
import com.wiko.emarket.vo.accceptance.ViewAcceptanceVO;
import com.wiko.emarket.vo.po.AreasVo;
import com.wiko.emarket.vo.po.CampaignBaseInfoVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AcceptanceDao {
    CampaignBaseInfoVo queryCampaignInfo(@Param("prId") String prId);

    /**
     *  通过Po ID查询po行信息
     * @param poId
     * @return
     */
    List<PoLineInfo> selectPoLineByPoId(@Param("poId") String poId);

    /**
     *  通过poId查询对应的prId
     * @param params
     * @return
     */
    String findPrByPoId(Map<String, Object> params);

    AreasVo getAreaListsByCampaignId(@Param("campaignId") String campaignId);

    ActivityHistoryInfoVo getActivityTypeListsByCampaignId(@Param("campaignId") String campaignId);

    List<String> roleListById(@Param("userId") Long userId);

    void updateAmountByFormId(@Param("totalMount") BigDecimal totalAmount,@Param("formId") String formId);

    List<FormPoLineInfo> getFromPoLineByPoIdAndFormId(@Param("poId") String poId, @Param("formId") String formId);

    /**
     *  查询当前campaign的验收详情
     *
     * @param map
     * @return
     */
    List<List<?>> queryAcceptanceByCampaignId(Map<String,Object> map);

    /**
     *  查询创建验收单列表
     *
     * @param map
     * @return
     */
    List<List<?>> creatAcceptanceList(Map<String,Object> map);

    /**
     *  查询我的验收单列表
     *
     * @param map
     * @return
     */
    List<List<?>> userAcceptanceList(Map<String,Object> map);

    /**
     *  创建验收单列表导出
     *
     * @param map
     * @return
     */
    List<AcceptanceDetailVO> exportCreatAcceptance(Map<String,Object> map);

    /**
     *  我的验收单列表导出
     *
     * @param map
     * @return
     */
    List<AcceptanceDetailVO> exportMyAcceptance(Map<String,Object> map);

    /**
     *  campaign详情页验收单列表导出
     *
     * @param map
     * @return
     */
    List<AcceptanceDetailVO> exportDetailAcceptance(Map<String,Object> map);

    /**
     *  查看验收单列表
     *
     * @param map
     * @return
     */
    List<List<?>> viewAcceptanceList(Map<String,Object> map);

    /**
     *  查看验收单列表导出
     *
     * @param map
     * @return
     */
    List<ViewAcceptanceVO> viewAcceptanceListExport(Map<String,Object> map);
}
