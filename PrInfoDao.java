package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.PrInfoEntity;
import com.wiko.emarket.vo.accceptance.AcceptanceFormLinkVo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @ClassName PrInfoDao
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/20 9:44
 * @Version 1.0
 **/
@Repository
public interface PrInfoDao extends BaseMapper<PrInfoEntity>{

    List<PrInfoEntity> queryAllPrIdByCampaign(String campaignId);

    void updatePrInfoByCampaign( List<PrInfoEntity> list);

    /**
     * 更新PR状态
     */
    void updatePrStatus (List<PrInfoEntity> prInfoList);

    /**
     * 根据LV3 主键id查询验收单信息
     *
     * @param ids
     * @return
     */
    AcceptanceFormLinkVo queryAcceptanceInfoByLv3UniqueId(Integer id);

    /**
     * 根据LV3 主键id查询campaignId
     *
     * @param ids
     * @return
     */
    AcceptanceFormLinkVo queryCampaignIdByLv3UniqueId(Integer id);
}
