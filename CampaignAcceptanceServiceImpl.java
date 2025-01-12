package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.utils.Dates;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.dao.SysDictDao;
import com.framework.modules.sys.dao.SysUserDao;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.shiro.ShiroUtils;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.AcceptanceDao;
import com.wiko.emarket.dao.AcceptanceFormInfoDao;
import com.wiko.emarket.dao.ActivityTypeDao;
import com.wiko.emarket.dao.PoLineInfoDao;
import com.wiko.emarket.entity.AcceptanceFormInfo;
import com.wiko.emarket.entity.ActivityType;
import com.wiko.emarket.service.campaign.CampaignAcceptanceService;
import com.wiko.emarket.service.emarketprocess.impl.CampaignCommonService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.accceptance.*;
import com.wiko.psi.util.CommonsUtils;
import com.wiko.psi.util.PageHelpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName CampaignAcceptanceServiceImpl
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/8/4 11:28
 * @Version 1.0
 **/
@Slf4j
@Service
public class CampaignAcceptanceServiceImpl implements CampaignAcceptanceService {
    @Autowired
    private AcceptanceDao acceptanceDao;
    @Autowired
    private ActivityTypeDao activityTypeDao;
    @Autowired
    private AcceptanceFormInfoDao acceptanceFormInfoDao;
    @Autowired
    private PoLineInfoDao poLineInfoDao;
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private SysDictDao sysDictDao;

    @Autowired
    private CampaignCommonService campaignCommonService;

    public static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public R creatAcceptanceList(Map<String, Object> params) {
        Integer page = (Integer) params.get("currentPage");
        Integer limit = (Integer) params.get("pageSize");
        params.put("page", (page - 1) * limit);
        params.put("limit", limit);
        params.put("currentUserId", ShiroUtils.getUserId());
        List<List<?>> lists = acceptanceDao.creatAcceptanceList(params);

        return getResponse(params, lists);
    }

    // 参数检验与构建分页参数
    private R getResponse(Map<String, Object> params, List<List<?>> lists) {
        if (lists == null || lists.get(0) == null) {
            return R.ok();
        }
        List<AcceptanceDetailVO> list = (List<AcceptanceDetailVO>) lists.get(0);
        Boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        buildAcceptanceDetailData(list, isChinese);
        lists.remove(0);
        lists.add(0, list);
        PageUtils pageUtils = PageHelpUtils.getPageUtils(params, lists);

        return R.ok().put("data", pageUtils);
    }

    @Override
    public void exportCreatAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException {
        params.put("currentUserId", ShiroUtils.getUserId());
        List<AcceptanceDetailVO> acceptanceDetailVOS = acceptanceDao.exportCreatAcceptance(params);
        buildExportCreatAcceptanceData(params, response, acceptanceDetailVOS);

    }

    @Override
    public R userAcceptanceList(Map<String, Object> params) {
        Integer page = (Integer) params.get("currentPage");
        Integer limit = (Integer) params.get("pageSize");
        params.put("page", (page - 1) * limit);
        params.put("limit", limit);
        params.put("currentUserId", ShiroUtils.getUserId());
        List<List<?>> lists = acceptanceDao.userAcceptanceList(params);
        return getResponse(params, lists);
    }

    @Override
    public void exportUserAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException {
        params.put("currentUserId", ShiroUtils.getUserId());
        List<AcceptanceDetailVO> acceptanceDetailVOS = acceptanceDao.exportMyAcceptance(params);
        buildExportCreatAcceptanceData(params, response, acceptanceDetailVOS);
    }

    @Override
    public void exportDetailAcceptance(Map<String, Object> params, HttpServletResponse response) throws IOException {
        params.put("currentUserId", ShiroUtils.getUserId());
        List<AcceptanceDetailVO> acceptanceDetailVOS = acceptanceDao.exportDetailAcceptance(params);
        buildExportCreatAcceptanceData(params, response, acceptanceDetailVOS);
    }

    private void buildExportCreatAcceptanceData(Map<String, Object> params, HttpServletResponse response, List<AcceptanceDetailVO> acceptanceDetailVOS) throws IOException {
        Boolean isChinese = CommonConstant.ZH_CN_LANGUAGE.equals(params.get("language"));
        buildAcceptanceDetailData(acceptanceDetailVOS, isChinese);
        String title = "CreateAcceptance";
        String fileName = title + "_" + Dates.getDateString();
        ArrayList<ExportCampaignAcceptanceEnVo> exportCampaignAcceptanceEnVos = new ArrayList<>();
        for (AcceptanceDetailVO acceptanceDetailVO : acceptanceDetailVOS) {
            ExportCampaignAcceptanceEnVo exportCampaignAcceptanceEnVo = new ExportCampaignAcceptanceEnVo();
            BeanUtils.copyProperties(acceptanceDetailVO, exportCampaignAcceptanceEnVo);
            exportCampaignAcceptanceEnVos.add(exportCampaignAcceptanceEnVo);
        }
        if (CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))) {
            CommonsUtils.exportExcel(title, fileName, exportCampaignAcceptanceEnVos, ExportCampaignAcceptanceEnVo.class, response);
        } else {
            CommonsUtils.exportExcel(title, fileName, exportCampaignAcceptanceEnVos, ExportCampaignAcceptanceVo.class, response);
        }
    }

    @Override
    public R queryAcceptanceByCampaignId(Map<String, Object> params) {
        Integer page = (Integer) params.get("currentPage");
        Integer limit = (Integer) params.get("pageSize");
        params.put("page", (page - 1) * limit);
        params.put("limit", limit);
        List<List<?>> lists = acceptanceDao.queryAcceptanceByCampaignId(params);
        return getResponse(params, lists);
    }

    @Override
    public R viewAcceptanceList(Map<String, Object> params) {
        Integer page = (Integer) params.get("currentPage");
        Integer limit = (Integer) params.get("pageSize");
        params.put("page", (page - 1) * limit);
        params.put("limit", limit);
        params.put("currentUserId", ShiroUtils.getUserId());
        List<List<?>> lists = acceptanceDao.viewAcceptanceList(params);
        if (lists == null || lists.get(0) == null) {
            return R.ok();
        }
        List<ViewAcceptanceVO> list = (List<ViewAcceptanceVO>) lists.get(0);
        Boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        buildViewAcceptanceData(list, isChinese);

        lists.remove(0);
        lists.add(0, list);
        PageUtils pageUtils = PageHelpUtils.getPageUtils(params, lists);

        return R.ok().put("data", pageUtils);
    }

    @Override
    public void viewAcceptanceExport(Map<String, Object> params, HttpServletResponse response) throws IOException {
        params.put("currentUserId", ShiroUtils.getUserId());
        List<ViewAcceptanceVO> viewAcceptanceVOS = acceptanceDao.viewAcceptanceListExport(params);
        Boolean isChinese = CommonConstant.ZH_CN_LANGUAGE.equals(params.get("language"));
        buildViewAcceptanceData(viewAcceptanceVOS, isChinese);
        // 设置文件名称
        String title = "ViewAcceptance";
        String fileName = title + "_" + Dates.getDateString();
        ArrayList<ViewAcceptanceExportVO> exportViewAcceptanceVos = new ArrayList<>();
        // 将导出对象进行适配
        for (ViewAcceptanceVO viewAcceptanceVO : viewAcceptanceVOS) {
            ViewAcceptanceExportVO exportViewAcceptanceVo = new ViewAcceptanceExportVO();
            BeanUtils.copyProperties(viewAcceptanceVO, exportViewAcceptanceVo);
            exportViewAcceptanceVos.add(exportViewAcceptanceVo);
        }
        if (CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))) {
            CommonsUtils.exportExcel(title, fileName, exportViewAcceptanceVos, ViewAcceptanceExportEnVO.class, response);
        } else {
            CommonsUtils.exportExcel(title, fileName, exportViewAcceptanceVos, ViewAcceptanceExportVO.class, response);
        }

    }

    // 构建查看验收单数据
    private void buildViewAcceptanceData(List<ViewAcceptanceVO> list, Boolean isChinese) {
        Map<String, SysUserEntity> sysUserEntityMap = new HashMap<>();
        // 构建支付方式映射关系
        Wrapper<SysDictEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("type", CommonConstant.PAY_TYPE);
        List<SysDictEntity> sysDictEntities = sysDictDao.selectList(wrapper);
        Map<String, String> paymentTypeMap;
        Map<String, SysDictEntity> purchaseTypeMap = new HashMap<>();
        Wrapper<SysDictEntity> purchaseWrapper = new EntityWrapper<>();
        purchaseWrapper.eq("type", CommonConstant.PURCHASE_TYPE);
        List<SysDictEntity> purchaseEntities = sysDictDao.selectList(purchaseWrapper);
        for (SysDictEntity sysDictEntity : purchaseEntities) {
            purchaseTypeMap.put(sysDictEntity.getValue(), sysDictEntity);
        }
        // 国际化设置
        if (isChinese) {
            paymentTypeMap = sysDictEntities.stream().collect(Collectors.toMap(SysDictEntity::getCode, SysDictEntity::getValue));
        } else {
            paymentTypeMap = sysDictEntities.stream().collect(Collectors.toMap(SysDictEntity::getCode, SysDictEntity::getValueEn));

        }

        boolean checkRoleName = campaignCommonService.checkSysUserRole(ShiroUtils.getUserId(),
                Arrays.asList(RoleNameEnum.ACTIVITY_CREATOR.getRoleNameEn()));

        // 当前登录用户
        Long currentUserId = ShiroUtils.getUserId();

        for (ViewAcceptanceVO vo : list) {
            // 设置当前验收人
            if ("1".equals(vo.getStatus())) {
                vo.setCurrentOwner(buildUserCord(sysUserEntityMap, vo.getFirstReviewer()));
            } else if ("2".equals(vo.getStatus())) {
                vo.setCurrentOwner(buildUserCord(sysUserEntityMap, vo.getSecondApprover()));
            } else {
                vo.setCurrentOwner(buildUserCord(sysUserEntityMap, vo.getCreatedById()));
            }
            // 设置campaignOwner名片
            vo.setCampaignOwner(buildUserCord(sysUserEntityMap, vo.getCampaignOwner()));
            // 设置campaignPa名片
            vo.setCampaignPa(buildUserCord(sysUserEntityMap, vo.getCampaignPa()));
            // 设置创建人名片
            vo.setCreatedBy(buildUserCord(sysUserEntityMap, vo.getCreatedById()));
            // 设置最后更新人名片
            vo.setUpdatedBy(buildUserCord(sysUserEntityMap, vo.getUpdatedBy()));
            // 设置支付方式名称
            vo.setPaymentWay(paymentTypeMap.get(vo.getPaymentType()));
            // 设置采购方式名称
            if (isChinese) {
                vo.setPurchaseWay(vo.getPurchaseType());
                vo.setPurchaseType(purchaseTypeMap.get(vo.getPurchaseType()).getCode());
            } else {
                vo.setPurchaseWay(purchaseTypeMap.get(vo.getPurchaseType()).getValueEn());
                vo.setPurchaseType(purchaseTypeMap.get(vo.getPurchaseType()).getCode());
            }
            if("1".equals(vo.getPurchaseType())){
                vo.setAcceptanceQty("--");
            }
            // 设置验收状态对应名称
            vo.setAcceptanceStatus(isChinese ? AcceptanceFormStatusEnum.getStatusNameCnByCode(vo.getStatus()) : AcceptanceFormStatusEnum.getStatusNameEnByCode(vo.getStatus()));
            // 设置是否显示取消按钮， 当前登录用户是创建人 + 状态 + 活动执行人角色
            vo.setIsShowCancelBtn(StringUtils.equals(currentUserId.toString(), vo.getCreatedById()) && AcceptanceFormStatusEnum.canCancelAcceptanceStatusCodes().contains(vo.getStatus()) && checkRoleName);
            // 设置是否显示取消按钮,要求当前用户是创建人有执行人角色，且验收单是草稿状态
            vo.setIsShowDelete(AcceptanceFormStatusEnum.DRAFT.getStatusCode().equals(vo.getStatus()) &&
                    vo.getCreatedById().equals(ShiroUtils.getUserId().toString()) &&
                    checkRoleName);
            // 设置编辑按钮是否显示(草稿、驳回、并且是活动执行人)
            vo.setIsShowEditBtn(AcceptanceFormStatusEnum.canEditAcceptanceStatusCodes().contains(vo.getStatus()) && StringUtils.equals(currentUserId.toString(), vo.getActivityExecutor()));

            // 设置pr、po中英文状态
            vo.setPrStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(vo.getPrStatus()) : PrPoStatusEnum.getNameEnByCode(vo.getPrStatus()));
            vo.setPoStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(vo.getPoStatus()) : PrPoStatusEnum.getNameEnByCode(vo.getPoStatus()));
        }
    }

    private String buildUserCord(Map<String, SysUserEntity> sysUserEntityMap, String userId) {
        if (userId == null) {
            // 为空不处理
            return null;
        }
        if (!sysUserEntityMap.containsKey(userId)) {
            SysUserEntity campaignOwnerVO = sysUserDao.selectById(userId);
            sysUserEntityMap.put(userId, campaignOwnerVO);
        }
        if (null == sysUserEntityMap.get(userId)) {
            return null;
        }

        return sysUserEntityMap.get(userId).getUserCard();
    }

    private String setUserCard (String userName, String employeeName) {
        StringBuffer userCardSb = new StringBuffer();

        if (StringUtils.isNotEmpty(userName)) {
            userCardSb.append(userName);
        }
        if (StringUtils.isNotEmpty(employeeName)) {
            userCardSb.append(" ").append(employeeName);
        }
        return userCardSb.toString();
    }

    @Override
    public R queryUserCordByUserName(Map<String, Object> params) {
        List<SysUserEntity> sysUserEntities = sysUserDao.selectLikeByUserName(params);
        if (sysUserEntities == null) {
            return R.ok();
        }
        List<UserIdCordMapVO> executorMapVOS = new ArrayList<>();
        for (SysUserEntity sysUserEntity : sysUserEntities) {
            UserIdCordMapVO executorMapVO = new UserIdCordMapVO();
            executorMapVO.setUserId(sysUserEntity.getUserId());
            StringBuffer userCordSb = new StringBuffer();
            if (StringUtils.isNotEmpty(sysUserEntity.getUsername())) {
                userCordSb.append(sysUserEntity.getUsername());
            }
            if (StringUtils.isNotEmpty(sysUserEntity.getEmployeeName())) {
                userCordSb.append(" ").append(sysUserEntity.getEmployeeName());
            }
            executorMapVO.setUserCord(userCordSb.toString().trim());
            executorMapVO.setUserNameEn(sysUserEntity.getUsername());
            executorMapVO.setUserNameCn(sysUserEntity.getEmployeeName());
            executorMapVOS.add(executorMapVO);
        }
        return R.ok().put("data", executorMapVOS);
    }

    // 设置创建验收单对应参数
    private void buildAcceptanceDetailData(List<AcceptanceDetailVO> list, Boolean isChinese) {
        // 用于构建名片时避免重复获取用户
        Map<String, SysUserEntity> sysUserEntityMap = new HashMap<>();
        List<ActivityType> activityTypes = activityTypeDao.selectList(null);
        Map<String, String> activityTypeMap = new HashMap<>();
        Wrapper<SysDictEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("type", CommonConstant.PAY_TYPE);
        List<SysDictEntity> sysDictEntities = sysDictDao.selectList(wrapper);
        Map<String, String> paymentTypeMap;
        Map<String, SysDictEntity> purchaseTypeMap = new HashMap<>();
        Wrapper<SysDictEntity> purchaseWrapper = new EntityWrapper<>();
        purchaseWrapper.eq("type", CommonConstant.PURCHASE_TYPE);
        List<SysDictEntity> purchaseEntities = sysDictDao.selectList(purchaseWrapper);
        for (SysDictEntity sysDictEntity : purchaseEntities) {
            purchaseTypeMap.put(sysDictEntity.getValue(), sysDictEntity);
        }
        // 国际化设置
        if (isChinese) {
            activityTypeMap = activityTypes.stream().collect(Collectors.toMap(ActivityType::getCode, ActivityType::getNameCn));
            paymentTypeMap = sysDictEntities.stream().collect(Collectors.toMap(SysDictEntity::getCode, SysDictEntity::getValue));
        } else {
            activityTypeMap = activityTypes.stream().collect(Collectors.toMap(ActivityType::getCode, ActivityType::getNameEn));
            paymentTypeMap = sysDictEntities.stream().collect(Collectors.toMap(SysDictEntity::getCode, SysDictEntity::getValueEn));
        }
        Long userId = ShiroUtils.getUserId();

        for (AcceptanceDetailVO acceptanceDetailVO : list) {
            // 设置营销科目名称
            acceptanceDetailVO.setSubjectNameLv1(activityTypeMap.get(acceptanceDetailVO.getSubjectLv1()));
            acceptanceDetailVO.setSubjectNameLv2(activityTypeMap.get(acceptanceDetailVO.getSubjectLv2()));
            acceptanceDetailVO.setSubjectNameLv3(activityTypeMap.get(acceptanceDetailVO.getSubjectLv3()));
            // 设置支付方式名称
            acceptanceDetailVO.setPaymentWay(paymentTypeMap.get(acceptanceDetailVO.getPaymentType()));
            // 设置执行人名片
            acceptanceDetailVO.setExecutor(setUserCard(acceptanceDetailVO.getUserName(), acceptanceDetailVO.getEmployeeName()));

            // 设置campaignOwner名片
            acceptanceDetailVO.setCampaignOwner(buildUserCord(sysUserEntityMap, acceptanceDetailVO.getCampaignOwner()));
            // 设置采购方式
            if (isChinese) {
                acceptanceDetailVO.setPurchaseWay(acceptanceDetailVO.getPurchaseType());
                acceptanceDetailVO.setPurchaseType(purchaseTypeMap.get(acceptanceDetailVO.getPurchaseType()).getCode());
            } else {
                acceptanceDetailVO.setPurchaseWay(purchaseTypeMap.get(acceptanceDetailVO.getPurchaseType()).getValueEn());
                acceptanceDetailVO.setPurchaseType(purchaseTypeMap.get(acceptanceDetailVO.getPurchaseType()).getCode());
            }

            // 设置 PR和PO状态
            String prStatusName = isChinese ? PrPoStatusEnum.getNameCnByCode(acceptanceDetailVO.getPrStatus()) : PrPoStatusEnum.getNameEnByCode(acceptanceDetailVO.getPrStatus());
            String poStatusName = isChinese ? PrPoStatusEnum.getNameCnByCode(acceptanceDetailVO.getPoStatus()) : PrPoStatusEnum.getNameEnByCode(acceptanceDetailVO.getPoStatus());
            acceptanceDetailVO.setPrStatusName(prStatusName);
            acceptanceDetailVO.setPoStatusName(poStatusName);

            // 如果存在pr没有PO则不进行累加，非自行采购的没有PO的也不能验收
            if (StringUtils.isEmpty(acceptanceDetailVO.getPoId())) {
                if (!"1".equals(acceptanceDetailVO.getPurchaseType())) {
                    // 不是自行采购
                    continue;
                }
                // 其它就是自行采购， 判断当前用户是否能够验收
                // 自行采购判断当前用户是否是验收人，不是则不显示验收按钮
                if (!acceptanceDetailVO.getUserId().equals(userId.toString())) {
                    continue;
                }
                Wrapper<AcceptanceFormInfo> acceptanceFormInfoWrapper = new EntityWrapper<>();
                acceptanceFormInfoWrapper.eq("delete_status", CommonConstant.INT_AVAILABLE);
                acceptanceFormInfoWrapper.eq("activity_id", acceptanceDetailVO.getActivityIdLv3());
                acceptanceFormInfoWrapper.ne("status", AcceptanceFormStatusEnum.CANCELLED.getStatusCode());
                // 当前LV3没有PO或者自行采购有效验收单不进行验收
                List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoDao.selectList(acceptanceFormInfoWrapper);
                if (acceptanceFormInfos.isEmpty()) {
                    // 没有验收过就可以验收, 自行采购一经生成PR就是关闭状态了，去掉校验!PrPoStatusEnum.CLOSED.getCode().equals(acceptanceDetailVO.getPrStatus()
                    acceptanceDetailVO.setIsAccepted(CommonConstant.NORMAL);
                }
            } else {
                // 有PO的情况下卷积全部金额和已验收金额
                sumPoAcceptanceAmount(acceptanceDetailVO, userId);
            }
        }
    }

    private void sumPoAcceptanceAmount(AcceptanceDetailVO acceptanceDetailVO, Long userId) {
        // 卷积已验收PO行信息
        Wrapper<AcceptanceFormInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("po_id", acceptanceDetailVO.getPoId());
        List<String> status = new ArrayList<>();
        // 待验收，待复核，审批完成
        status.add(AcceptanceFormStatusEnum.WAITING_REVIEW.getStatusCode());
        status.add(AcceptanceFormStatusEnum.WAITING_APPROVAL.getStatusCode());
        status.add(AcceptanceFormStatusEnum.APPROVAL_SUCCESS.getStatusCode());
        wrapper.in("status", status);
        wrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
        // 卷积已验收金额
        List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoDao.selectList(wrapper);
        // 卷积全部金额
        BigDecimal allAmount = poLineInfoDao.queryAcceptanceAmount(acceptanceDetailVO.getPoId());
        if (allAmount == null) {
            allAmount = ZERO;
        }
        acceptanceDetailVO.setPoAmount(allAmount);
        if (acceptanceFormInfos.size() > 0) {
            BigDecimal acceptanceAmount = acceptanceFormInfos
                    .stream()
                    .map(AcceptanceFormInfo::getAcceptanceAmount)
                    .reduce(BigDecimal::add)
                    .get();
            acceptanceDetailVO.setAcceptanceAmount(acceptanceAmount);
        } else {
            acceptanceDetailVO.setAcceptanceAmount(ZERO);
        }
        // 剩余验收数量
        acceptanceDetailVO.setSurplusAmount(allAmount.subtract(acceptanceDetailVO.getAcceptanceAmount()));
        boolean isNormal4PoStatus = !PrPoStatusEnum.CLOSED.getCode().equalsIgnoreCase(acceptanceDetailVO.getPoStatus()); // 非关闭的， 因为POstatus是定时任务执行的会有延时
        if (isNormal4PoStatus && (userId.toString().equals(acceptanceDetailVO.getUserId())) && acceptanceDetailVO.getSurplusAmount().compareTo(ZERO) == 1 && acceptanceDetailVO.getPoAmount().compareTo(ZERO) == 1) {
            // 是否能够验收， Y能验收， N不能验收,   （PO有效, 登录人=活动执行人， 剩余验收金额和数量大于0）
            acceptanceDetailVO.setIsAccepted(CommonConstant.NORMAL);
        }
    }

    @Override
    public R queryAcceptanceFormStatus() {
        EnumSet<AcceptanceFormStatusEnum> enumSet = EnumSet.allOf(AcceptanceFormStatusEnum.class);

        List<Map<String, Object>> acceptanceFormStatus = new ArrayList<>();
        for (AcceptanceFormStatusEnum acpEnum : enumSet) {
            if (AcceptanceFormStatusEnum.DELETED.getStatusCode().equals(acpEnum.getStatusCode())) {
                // 需要排除的验收状态
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("statusCode", acpEnum.getStatusCode());
            map.put("statusNameCn", acpEnum.getStatusNameCn());
            map.put("statusNameEn", acpEnum.getStatusNameEn());
            acceptanceFormStatus.add(map);
        }

        return R.ok().put("data", acceptanceFormStatus);
    }

    @Override
    public R checkAcceptor(Map<String,Object> params) {
        Long userId = ShiroUtils.getUserId();
        if (params.containsKey("activityExecutorUserId") && params.containsKey("lv3ActivityId")){
            // 因为此逻辑多处调用，适配入参
            params.put("userId", params.get("activityExecutorUserId"));
            params.put("activityIdLv3", params.get("lv3ActivityId"));
        }
        // 判断当前用户是否是当前单的执行人
        if (!(userId.toString()).equals(params.get("userId"))) {
            return R.error(StatusCodeEnum.NO_PERMISSIONS.getStatusCode(), I18nUtil.getMessage("NoPermissionOperate"));
        }
        // 判断当前用户是否存在执行人权限
        List<String> roleNames = acceptanceDao.roleListById(userId);
        int index = (int) roleNames
                .stream()
                .filter("Activity Creator"::equals)
                .count();
        if (index == 0) {
            // 0 则是活动执行人
            return R.error(StatusCodeEnum.NO_PERMISSIONS.getStatusCode(), I18nUtil.getMessage("NoPermissionOperate"));
        }
        // 如果是自行采购，存在验收单也不允许进行验收
        if (CommonConstant.PURCHASE_TYPE_SELF_CN.equals(params.get("purchaseType")) || CommonConstant.PURCHASE_TYPE_SELF_EN.equals(params.get("purchaseType"))) {
            Wrapper<AcceptanceFormInfo> acceptanceFormInfoWrapper = new EntityWrapper<>();
            acceptanceFormInfoWrapper.eq("delete_status", 1);
            acceptanceFormInfoWrapper.ne("status", AcceptanceFormStatusEnum.CANCELLED.getStatusCode());
            acceptanceFormInfoWrapper.eq("activity_id", params.get("activityIdLv3"));
            // 当前LV3没有PO或者自行采购有效验收单不进行验收
            List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoDao.selectList(acceptanceFormInfoWrapper);
            // 如果存在有效自行验收单也只会有一条
            if (!acceptanceFormInfos.isEmpty()) {
                return R.error(StatusCodeEnum.DATA_EXISTS.getStatusCode(), I18nUtil.getMessage("PRHasAcceptance"))
                        .put("data", acceptanceFormInfos.get(0));
            }
        }
        return R.ok();
    }

    @Override
    public R deleteAcceptance(Long id) {
        Long userId = ShiroUtils.getUserId();
        boolean checkRoleName = campaignCommonService.checkSysUserRole(ShiroUtils.getUserId(),
                Arrays.asList(RoleNameEnum.ACTIVITY_CREATOR.getRoleNameEn()));
        // 没有执行人角色不能
        if (!checkRoleName) {
            return R.error(StatusCodeEnum.NO_PERMISSIONS.getStatusCode(), I18nUtil.getMessage("NoPermissionOperate"));
        }

        AcceptanceFormInfo acceptanceFormInfo = acceptanceFormInfoDao.selectById(id);
        // 已删除的不能重复删
        if (CommonConstant.NO_AVAILABLE.equals(acceptanceFormInfo.getDeleteStatus())) {
            return R.error(StatusCodeEnum.DATA_EXISTS.getStatusCode(), I18nUtil.getMessage("dataIsDeleted"));
        }
        // 非草稿的不能删除
        if (!AcceptanceFormStatusEnum.DRAFT.getStatusCode().equals(acceptanceFormInfo.getStatus())) {
            return R.error(StatusCodeEnum.DATA_EXISTS.getStatusCode(), I18nUtil.getMessage("noDraftIsNotDelete"));
        }
        // 非当前验收单创建人不能删除
        if (!userId.toString().equals(acceptanceFormInfo.getCreatedBy())) {
            return R.error(StatusCodeEnum.NO_PERMISSIONS.getStatusCode(), I18nUtil.getMessage("notCreatedByNotDelete"));
        }
        AcceptanceFormInfo info = new AcceptanceFormInfo();
        info.setId(id);
        info.setDeleteStatus(CommonConstant.NO_AVAILABLE);
        info.setStatus(AcceptanceFormStatusEnum.DELETED.getStatusCode());
        acceptanceFormInfoDao.updateById(info);
        return R.ok();
    }

}
