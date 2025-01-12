package com.wiko.emarket.service.campaign;

import com.framework.common.utils.R;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @ClassName CampaignAccepanceService
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/8/4 11:14
 * @Version 1.0
 **/
public interface CampaignAcceptanceService {
    /**
     * 创建验收单列表
     *
     * @param params Map<String,Object>
     * @return R
     */
    R creatAcceptanceList(Map<String, Object> params);

    /**
     * 导出创建验收单
     *
     * @param params   Map<String,Object>
     * @param response
     * @throws IOException
     */
    void exportCreatAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException;

    /**
     * 我的验收
     *
     * @param params Map<String,Object>
     * @return R
     */
    R userAcceptanceList(Map<String, Object> params);

    /**
     * 导出我的验收单
     *
     * @param params   Map<String,Object>
     * @param response
     * @throws IOException
     */
    void exportUserAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException;

    /**
     *
     *
     * @param params
     * @param response
     * @throws IOException
     */
    void exportDetailAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException;

    /**
     * 查看验收单-列表
     *
     * @param params Map<String,Object>
     * @return R
     */
    R viewAcceptanceList(Map<String, Object> params);

    /**
     * 导出查看验收单
     *
     * @param params   Map<String,Object>
     * @param response
     * @throws IOException
     */
    void viewAcceptanceExport(Map<String, Object> params, HttpServletResponse response) throws IOException;

    /**
     * 根据campaignId查询验收详情Tab
     *
     * @param params Map<String,Object>
     * @return R
     */
    R queryAcceptanceByCampaignId(Map<String, Object> params);

    /**
     * 根据用户账号模糊查询执行人
     *
     * @param params Map<String,Object>
     * @return R
     */
    R queryUserCordByUserName(Map<String, Object> params);

    /**
     * 验收状态枚举
     *
     * @return R
     */
    R queryAcceptanceFormStatus();

    /**
     * 校验验收人
     *
     * @param params
     * @return R
     */
    R checkAcceptor(Map<String,Object> params);

    /**
     * 删除验收单
     *
     * @param id
     * @return R
     */
    R deleteAcceptance(Long id);
}
