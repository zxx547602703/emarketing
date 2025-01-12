package com.wiko.emarket.dao;

import com.wiko.emarket.entity.AttachmentInfo;
import com.wiko.emarket.entity.SignatoryInfo;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.CampaignPrDetailsVO;
import com.wiko.emarket.vo.PrNumberRequestVO;
import com.wiko.emarket.vo.SignatoryInfoVo;
import com.wiko.emarket.vo.po.PoDetailsVO;
import com.wiko.emarket.vo.po.PolIneDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CampaignDao {
    /**
     *  根据campaignId查询附件列表
     * @param campaignId
     * @return
     */
    List<AttachmentInfo> selectAttachmentById(@Param("campaignId") String campaignId);

    List<String> getActivityPidByBudgetCode(@Param("budgetCode")String budgetCode);

    List<SignatoryInfoVo> selectApproveMinAmount(SignatoryInfo info);

    List<Integer> selectProcessIdById(@Param("campaignId")String campaignId);

    List<ActivityHistoryInfoVo> selectLv2(@Param("processId") Integer processId);
    /**
     *  根据prId查询PO详情
     * @param params
     * @return List
     */
    List<PoDetailsVO> getPoDetailsVoList(Map<String, Object> params);
    List<List<?>> queryPage(Map<String,Object> map);
    /**
     *  根据campaignId查询Pr详情
     * @param lv3Params
     * @return List
     */
    CampaignPrDetailsVO getPrDetails(Map<String, Object> lv3Params);
    /**
     *  根据poid查询po行详情
     * @param params
     * @return List
     */
    List<PolIneDetail> getPoDetails(Map<String, Object> params);
    /**
     *  根据campaign查询activity3的信息构建接口参数
     * @param params
     * @return List
     */
    List<PrNumberRequestVO> getPrNumberRequestList(Map<String, Object> params);



    List<Map<String,Object>> getApprovedCampaign();

    /**
     * 根据LV3 activityIds查询PO id
     *
     * @param activityIds
     * @return
     */
    List<String> getPobyActivityId(@Param("activityIds") List<String> activityIds);

    List<Integer> getMyCampaignId(Map<String, Object> params);

    /**
     * 查询L3验收金额汇总
     *
     * @param prId
     * @return
     */
    BigDecimal getL3AcceptancedAmountTotal (@Param("prId") String prId);
}
