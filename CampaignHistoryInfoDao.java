package com.wiko.emarket.dao;


import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.vo.CampaignExecuteInformationLv3Vo;
import com.wiko.emarket.vo.CampaignExecuteInformationVo;
import com.wiko.emarket.vo.CampaignHistoryInfoVo;
import com.wiko.emarket.vo.po.PoBillingedAmountDetailVo;
import com.wiko.emarket.vo.po.PoPaymentedAmountDetailVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CampaignHistoryInfoDao extends BaseMapper<CampaignHistoryInfoVo> {
    /**
     * 查询数据
     *
     * @param params
     * @return
     */
    List<CampaignHistoryInfoVo> findList(Map<String, Object> params);

    void updateApprover(CampaignHistoryInfoVo info);

    /**
     * campaign id 查询LV2执行信息
     *
     * @param id
     * @return
     */
    List<CampaignExecuteInformationVo> queryCampaignExecuteInformation4Lv2(@Param("id") Integer id);


    /**
     * campaign id 查询LV3执行信息
     *
     * @param id
     * @param lv2ActivityId
     * @return
     */
    List<CampaignExecuteInformationLv3Vo> queryCampaignExecuteInformation4Lv3(@Param("id") Integer id, @Param(
            "lv2ActivityId") String lv2ActivityId);


    /**
     * 查询L3验收金额汇总
     *
     * @param prId
     * @return
     */
    BigDecimal getL3AcceptancedAmountTotal (@Param("prId") String prId);

    /**
     * 查询PO总金额
     *
     * @param poId
     * @return
     */
    BigDecimal getPoAmountTotal (@Param("poId") String poId);

    /**
     * 查询PO已验收金额
     *
     * @param poId
     * @return
     */
    BigDecimal getPoAcceptancedAmountTotal (@Param("prId") String prId, @Param("poId") String poId);


    /**
     * 查询累计开票金额
     *
     * @param poId
     * @return
     */
    BigDecimal getPoBillingedAmountTotal (@Param("poId") String poId);

    /**
     * 查询累计付款金额
     *
     * @param poId
     * @return
     */
    BigDecimal getPoPaymentedAmountTotal (@Param("poId") String poId);


    /**
     * campaign id 查询LV3执行信息
     *
     * @param prId
     * @param poId
     * @return
     */
    CampaignExecuteInformationLv3Vo queryCampaignExecuteInformation4Po(@Param("prId") String prId,
                                                                       @Param("poId") String poId);


    /**
     * 根据poid查询po 开票详情
     *
     * @param params
     * @return List
     */
    List<PoBillingedAmountDetailVo> getPoBillingedDetails(Map<String, Object> params);

    /**
     * 根据poid查询po 开票详情
     *
     * @param params
     * @return List
     */
    List<PoPaymentedAmountDetailVo> getPoPaymentedDetails(Map<String, Object> params);
}
