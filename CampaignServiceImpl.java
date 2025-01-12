package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.exception.RRException;
import com.framework.common.utils.Dates;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysConfigService;
import com.framework.modules.sys.service.SysDictService;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.activiti.dao.ProcessBasicInfoDao;
import com.wiko.activiti.vo.ProcessBasicInfoVo;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.AcceptanceFormInfoDao;
import com.wiko.emarket.dao.CampaignDao;
import com.wiko.emarket.dao.PrInfoDao;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.FeishuMessage.IFeiShuSendMessageService;
import com.wiko.emarket.service.campaign.SapRfcService;
import com.wiko.emarket.service.campaign.*;
import com.wiko.emarket.service.emarketprocess.EmarketApplyService;
import com.wiko.emarket.service.emarketprocess.impl.CampaignCommonService;
import com.wiko.emarket.service.reviewconfigmanage.ApproverInfoService;
import com.wiko.emarket.service.reviewconfigmanage.SignatoryInfoService;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.MyConstant;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.util.SysDictAndParamMngKeys;
import com.wiko.emarket.vo.*;
import com.wiko.emarket.vo.po.PoDetailsVO;
import com.wiko.emarket.vo.po.PolIneDetail;
import com.wiko.psi.util.PageHelpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/28 17:21
 * @description: TODO
 **/
@Service("CampaignService")
@Slf4j
public class CampaignServiceImpl implements CampaignService {
    public static final String TRANSFER_ROLE = "campaignTransferRole";
    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private AreaService areaService;

    @Autowired
    private ActivityInfoService activityInfoService;

    @Autowired
    private AttachmentInfoService attachmentInfoService;

    @Autowired
    private EmarketApplyService emarketApplyService;

    @Autowired
    private ApproverInfoService approverInfoService;

    @Autowired
    private SignatoryInfoService signatoryInfoService;

    @Autowired
    private CampaignHistoryInfoServiceImpl campaignHistoryInfoService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private ActivityTypeService activityTypeService;
    @Autowired
    private SapRfcService sapRfcService;
    @Autowired
    private ActivityHistoryInfoService activityHistoryInfoService;

    @Autowired
    private PrInfoDao prInfoDao;

    @Autowired
    private AcceptanceFormInfoDao acceptanceFormInfoDao;

    @Autowired
    private CampaignCommonService campaignCommonService;

    @Autowired
    private SysDictService sysDictService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ProcessBasicInfoDao basicInfoDao;

    @Autowired
    private IFeiShuSendMessageService iFeiShuSendMessageService;

    @Autowired
    private SysConfigService sysConfigService;

    @Value("${campaign.detail.link.url}")
    private String campaignDetailLinkUrl;

    @Override
    @Transactional
    public R create(CampaignCreateVo campaignCreateVo) {
        log.info("emarket process create params:", campaignCreateVo);
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        String operator = SecurityUtils.getSubject().getPrincipal() == null ? null : ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId().toString();
        if (operator == null) {
            return R.error(401, isChinese ? TipEnum.NOTLOGIN.getStatusNameCn() : TipEnum.NOTLOGIN.getStatusNameEn());
        }
        // 校验用户是否具备campaignOwner或者PA角色
        List<String> checkRoleNames = Arrays.asList(RoleNameEnum.CAMPAIGN_PA.getRoleNameEn(), RoleNameEnum.CAMPAIGN_OWNER.getRoleNameEn());
        boolean roleCheckRes = campaignCommonService.checkSysUserRole(Long.parseLong(operator), checkRoleNames);
        if (!roleCheckRes) {
            return R.error(I18nUtil.getMessage("NotCampaignOwnerOrPaRole"));
        }

        // 驳回非当前处理人不能提交和保存
        String checkMsg = checkIsApplyUser(campaignCreateVo, operator);
        if (StringUtils.isNotEmpty(checkMsg)) {
            return R.error(checkMsg);
        }

//        campaignCreateVo.setYear("" + Calendar.getInstance().get(Calendar.YEAR));
        campaignCreateVo.setSubmitDate(new Date());
        String campaignSaveId = campaignCreateVo.getCampaignSaveId();
        String campaignId = Strings.isEmpty(campaignSaveId) && Strings.isEmpty(campaignCreateVo.getCampaignId()) ? getCampaignId().toString() : Strings.isNotEmpty(campaignSaveId) ? campaignSaveId : campaignCreateVo.getCampaignId();
        campaignCreateVo.setCampaignId(campaignId);

        // 1.校验campaign参数
        String checkRes = checkParam(campaignCreateVo, isChinese);
        if (StringUtils.isNotEmpty(checkRes)) {
            return R.error(checkRes);
        }
        // 2.检查 activityLV2/LV3具体参数
        R r1 = activityInfoService.checkParams(campaignCreateVo);
        if (!("0".equals(r1.get("code").toString()))) {
            return r1;
        }

        // 3.1 获取业务专家
        List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
        Map<Long, SysUserEntity> userIdMap = new HashMap<>();
        sysUserEntities.stream().filter(sysUserEntity -> !userIdMap.containsKey(sysUserEntity.getUserId())).forEach(sysUserEntity -> userIdMap.put(sysUserEntity.getUserId(), sysUserEntity));


        List<UserLikeVo> exportList =null;
        if(StringUtils.equals(campaignCreateVo.getActionScene(), CommonConstant.SPLIT_SCENE)){
            List<Integer> processIds = campaignDao.selectProcessIdById(campaignCreateVo.getCampaignId());
            Wrapper<CampaignHistoryInfoVo> wrapper = new EntityWrapper<>();
            wrapper.eq("campaign_id", campaignCreateVo.getCampaignId());
            wrapper.eq("process_id", processIds.get(0));
            CampaignHistoryInfoVo latestCampaign = campaignHistoryInfoService.selectOne(wrapper);
            CampaignHistoryInfoVo campaignHistoryInfoVo =
                    campaignHistoryInfoService.getCampaignHistoryDetail(latestCampaign.getCampaignId(),latestCampaign.getId());
            exportList =  getExportList(campaignHistoryInfoVo, userIdMap);
        }else{
            exportList =  getExportList(campaignCreateVo, userIdMap);
        }

        // 3.2验证权签人
        Map<String, List<UserLikeVo>> signatoryMap = (StringUtils.equals(campaignCreateVo.getActionScene(), CommonConstant.SPLIT_SCENE) ? new HashMap<>() : getSignatoryMap(campaignCreateVo, userIdMap));
        List<UserLikeVo> marketsignatoryList = null == signatoryMap.get(CommonConstant.MKT1) ? Collections.emptyList() : signatoryMap.get(CommonConstant.MKT1);
        List<UserLikeVo> retailsignatoryList = null == signatoryMap.get(CommonConstant.MKT2) ? Collections.emptyList() : signatoryMap.get(CommonConstant.MKT2);
        setCompaignVersion(campaignCreateVo);

        // 4.保存Campaign信息
        boolean isAddRecord = insertOrUpdateCampaignHis(campaignCreateVo, operator, CommonConstant.OPERATE_TYPE_SUBMIT);
        // 4.1.保存附件信息 1:N
        saveAttachment(campaignCreateVo, operator, isAddRecord);
        // 4.3.保存L2  L3 活动信息
        R result2 = activityHistoryInfoService.updateInfo(campaignCreateVo, CommonConstant.OPERATE_TYPE_SUBMIT, isAddRecord);
        if (!("0".equals(result2.get("code").toString()))) {
            return result2;
        }

        // 5.发起评审流程
        emarketApplyService.submit(((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId(), campaignCreateVo, exportList, marketsignatoryList, retailsignatoryList);

        // 异步推送飞书消息
        sendFeiShuMessageHandle4Submit(campaignCreateVo);

        return R.ok().put("campaignId", campaignCreateVo.getCampaignId()).put("processId", campaignCreateVo.getProcessId()).put("id", campaignCreateVo.getId());
    }

    /**
     * 驳回再提交， 要检查是否是申请人,  不是申请人不让提交
     *
     * @param campaignCreateVo
     * @return
     */
    private String checkIsApplyUser (CampaignCreateVo campaignCreateVo, String operator) {
        if (null == campaignCreateVo.getId()) {
            return null;
        }

        CampaignHistoryInfoVo info = campaignHistoryInfoService.selectById(campaignCreateVo.getId());
        if (!campaignCommonService.isReturnStatus(info.getStatus())) {
            return null;
        }
        ProcessBasicInfoVo basicInfoVo = basicInfoDao.selectOne(info.getProcessId());
        if (null != basicInfoVo && !StringUtils.equals(basicInfoVo.getApplyUser(), operator)) {
            return I18nUtil.getMessage("NotCurrentHandler");
        }

        return null;
    }


    private void setCompaignVersion(CampaignCreateVo campaignCreateVo) {
        // 获取最大的有效版本
        CampaignHistoryInfoVo latestVersionCampaign =
                campaignHistoryInfoService.getCampaignLatestValidVersion(campaignCreateVo.getCampaignId());
        if (latestVersionCampaign == null) {
            campaignCreateVo.setVersion(CommonConstant.CAMPAIGN_INIT_VERSION);
            return;
        }

        // 只有变更才会产生版本变更
        if (CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(latestVersionCampaign.getStatus())) {
            if (CommonConstant.SPLIT_SCENE.equals(campaignCreateVo.getActionScene())) {
                // 拆分入口不走权签， 版本号加0.01
                BigDecimal vNum = new BigDecimal(latestVersionCampaign.getVersion().substring(1)).add(BigDecimal.valueOf(CommonConstant.CAMPAIGN_SMALL_VERSION_INCREMENT));
                campaignCreateVo.setVersion("v" + vNum.toString());
                return;
            } else {
                // 变更入口走权签， 版本号加1.0
                BigDecimal vNum = BigDecimal.valueOf(Math.floor(Double.parseDouble(latestVersionCampaign.getVersion().substring(1))))
                        .add(BigDecimal.valueOf(CommonConstant.CAMPAIGN_BIG_VERSION_INCREMENT));
                campaignCreateVo.setVersion("v" + vNum.toString());
                return;
            }
        }

        campaignCreateVo.setVersion(latestVersionCampaign.getVersion());
    }

    private boolean insertOrUpdateCampaignHis(CampaignCreateVo campaignCreateVo, String operator, String operateType) {
        boolean isAddRecord = false;
        if (null == campaignCreateVo.getId()) {
            insertCampaignTableHis(operator, campaignCreateVo, operateType);
            isAddRecord = true;
            return isAddRecord;
        }

        CampaignHistoryInfoVo dbInfo = campaignHistoryInfoService.selectById(campaignCreateVo.getId());
        if (StringUtils.equals(CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode(), dbInfo.getStatusCode()) || StringUtils.equals(CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode(), dbInfo.getStatus())) {
            // 除了进来查询的campaign状态是已审批通过的， 是走变更， 其余场景都是编辑
            // 其余场景新增campaign记录  230222发现bug变更被驳回-保存-提交时新增了campaign流程， 先判断新增其余都作为更新
            insertCampaignTableHis(operator, campaignCreateVo, operateType);
            isAddRecord = true;
        } else {
            // 更新campaign记录,  编辑按钮进入、变更按钮进入但是多次保存， 复制按钮进入但是多次保存
            updateCampaignTableHis(operator, campaignCreateVo, operateType);
        }
        return isAddRecord;

    }

    // 获取权签人
    private Map<String, List<UserLikeVo>> getSignatoryMap(CampaignCreateVo campaignCreateVo, Map<Long, SysUserEntity> userIdMap) {
        Map<String, List<UserLikeVo>> signatoryMap = this.getSignatoryList(campaignCreateVo, null, userIdMap);

        return signatoryMap;
    }

    // 获取业务专家
    private List<UserLikeVo> getExportList(CampaignCreateVo campaignCreateVo, Map<Long, SysUserEntity> userIdMap) {
        // 业务专家
        List<UserLikeVo> exportList = new ArrayList<>();
        // 5.1验证业务管理员是否存在
        Map<String, Object> approverRetMap = this.getApproverInfos(campaignCreateVo);
        if (!approverRetMap.get("msg").equals("success")) {
            throw new RRException(approverRetMap.get("msg").toString());
        }
        List<ApproverInfoVo> approverInfos = (List<ApproverInfoVo>) approverRetMap.get("data");
        if (approverInfos.size() == 0) {
            throw new RRException(I18nUtil.getMessage("NoConfigSalesperson"));
        } else {
            // 遍历approveInfos，通过userId获取到用户列表信息
            Map<String, ApproverInfoVo> approverInfoVoMap = new HashMap<>();
            approverInfos.forEach(approverInfo -> {
                approverInfoVoMap.put(approverInfo.getUserId(), approverInfo);
            });

            approverInfoVoMap.forEach((k, v) -> {
                UserLikeVo userLikeVo = new UserLikeVo();
                userLikeVo.setUsername(v.getUserName());
                userLikeVo.setUserId(Long.parseLong(k));
                SysUserEntity sysUser = userIdMap.get(Long.parseLong(k));
                userLikeVo.setMobile(null == sysUser ? null : sysUser.getMobile());
                exportList.add(userLikeVo);
            });
        }
        return exportList;
    }

    private List<UserLikeVo> getExportList(CampaignHistoryInfoVo campaignHistoryInfoVo, Map<Long, SysUserEntity> userIdMap) {
        // 业务专家
        List<UserLikeVo> exportList = new ArrayList<>();
        // 5.1验证业务管理员是否存在
        // 将CampaignHistoryInfoVo转换成 CampaignCreateVo
        Map<String, Object> approverRetMap = this.getApproverInfos(campaignHistoryInfoVo);
        if (!approverRetMap.get("msg").equals("success")) {
            throw new RRException(approverRetMap.get("msg").toString());
        }
        List<ApproverInfoVo> approverInfos = (List<ApproverInfoVo>) approverRetMap.get("data");
        if (approverInfos.size() == 0) {
            throw new RRException(I18nUtil.getMessage("NoConfigSalesperson"));
        } else {
            // 遍历approveInfos，通过userId获取到用户列表信息
            Map<String, ApproverInfoVo> approverInfoVoMap = new HashMap<>();
            approverInfos.forEach(approverInfo -> {
                approverInfoVoMap.put(approverInfo.getUserId(), approverInfo);
            });

            approverInfoVoMap.forEach((k, v) -> {
                UserLikeVo userLikeVo = new UserLikeVo();
                userLikeVo.setUsername(v.getUserName());
                userLikeVo.setUserId(Long.parseLong(k));
                SysUserEntity sysUser = userIdMap.get(Long.parseLong(k));
                userLikeVo.setMobile(null == sysUser ? null : sysUser.getMobile());
                exportList.add(userLikeVo);
            });
        }
        return exportList;
    }


    public Map<String, List<UserLikeVo>> getSignatoryList(CampaignCreateVo campaignCreateVo, List<ActivityHistoryInfoVo> activitys, Map<Long, SysUserEntity> userIdMap) {
        Map<String, List<UserLikeVo>> retMap = new HashMap<>();
        List<ActivityHistoryInfoVo> activityInfos = campaignCreateVo.getActivityInfos();
        //将业务科目LV1的activity的金额累加
        Map<String, BigDecimal> map = getLv2SubjectAmount(activityInfos);
        Map<String, BigDecimal> mapHis = null;
        Map<String, Boolean> compareMap = null;

        if (null != activitys) {
            mapHis = getLv2SubjectAmount(activitys);
            compareMap = compareHis(map, mapHis);
            if (null != compareMap) {
                for (Map.Entry<String, Boolean> entry : compareMap.entrySet()) {
                    if (!entry.getValue()) {
                        List<SignatoryInfo> everySignatory = getEverySubjectSignatory(campaignCreateVo, entry.getKey(), map.get(entry.getKey()));
                        List<SignatoryInfo> list = orderBy(everySignatory);
                        List<SignatoryInfo> signatoryInfos = duplicateRemoval(list);
                        if (signatoryInfos.size() > 3) {
                            ActivityType activityType = getName(entry.getKey());
                            AreaEntity entity = getAreaName(campaignCreateVo.getRepresentative());
                            if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                                throw new RRException(String.format(I18nUtil.getMessage("TooManySignatory"), entity.getNameEn(), activityType.getNameEn()));
                            } else {
                                throw new RRException(String.format(I18nUtil.getMessage("TooManySignatory"), entity.getNameCn(), activityType.getNameCn()));
                            }
                        }
                        List<UserLikeVo> userLikeVo = new ArrayList<>();
                        for (int i = signatoryInfos.size() - 1; i >= 0; i--) {
                            SignatoryInfo info = signatoryInfos.get(i);
                            UserLikeVo vo = getUserLikeVoByUserId(Long.parseLong(info.getUserId()), userIdMap);
                            userLikeVo.add(vo);
                        }
                        retMap.put(entry.getKey(), userLikeVo);
                    }
                }
            }
        } else {
            for (Map.Entry<String, BigDecimal> entry : map.entrySet()) {
                List<SignatoryInfo> everySignatory = getEverySubjectSignatory(campaignCreateVo, entry.getKey(), map.get(entry.getKey()));
                List<SignatoryInfo> list = orderBy(everySignatory);
                List<SignatoryInfo> signatoryInfos = duplicateRemoval(list);
                if (signatoryInfos.size() > 3) {
                    ActivityType activityType = getName(entry.getKey());
                    AreaEntity entity = getAreaName(campaignCreateVo.getRepresentative());
                    if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                        throw new RRException(String.format(I18nUtil.getMessage("TooManySignatory"), entity.getNameEn(), activityType.getNameEn()));
                    } else {
                        throw new RRException(String.format(I18nUtil.getMessage("TooManySignatory"), entity.getNameCn(), activityType.getNameCn()));
                    }
                }
                List<UserLikeVo> userLikeVo = new ArrayList<>();
                for (int i = signatoryInfos.size() - 1; i >= 0; i--) {
                    SignatoryInfo info = signatoryInfos.get(i);
                    UserLikeVo vo = getUserLikeVoByUserId(Long.parseLong(info.getUserId()), userIdMap);
                    userLikeVo.add(vo);
                }
                retMap.put(entry.getKey(), userLikeVo);
            }
        }
        return retMap;
    }


    public List<SignatoryInfo> orderBy(List<SignatoryInfo> signatoryInfos) {
        List<SignatoryInfo> collect = signatoryInfos.stream().sorted(Comparator.comparing(SignatoryInfo::getMaxAmount)).collect(Collectors.toList());
        return collect;
    }

    public UserLikeVo getUserLikeVoByUserId(Long userId, Map<Long, SysUserEntity> userIdMap) {
        Wrapper<SysUserEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        SysUserEntity userEntity = userService.selectOne(wrapper);
        UserLikeVo userLikeVo = new UserLikeVo();
        userLikeVo.setUserId(userEntity.getUserId());
        userLikeVo.setUsername(userEntity.getUsername());
        SysUserEntity sysUser = userIdMap.get(userId);
        userLikeVo.setMobile(null == sysUser ? null : sysUser.getMobile());
        return userLikeVo;
    }

    public List<SignatoryInfo> duplicateRemoval(List<SignatoryInfo> everySignatory) {
        List<SignatoryInfo> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (int i = everySignatory.size() - 1; i >= 0; i--) {
            String userId = everySignatory.get(i).getUserId();
            if (!set.contains(userId)) {
                set.add(userId);
                list.add(everySignatory.get(i));
            }
        }
        return list;
    }

    public List<SignatoryInfo> getEverySubjectSignatory(CampaignCreateVo campaignCreateVo, String lv1Code, BigDecimal amount) {
        List<SignatoryInfo> retList = new ArrayList<>();
        //代表处
        List<SignatoryInfo> signatoryRoInfos = getSignatorys(lv1Code, campaignCreateVo.getRepresentative());
        boolean roFlag = addSignatory(retList, signatoryRoInfos, amount);
        if (!roFlag) {
            return retList;
        }
        // 地区部
        List<SignatoryInfo> signatoryRGInfos = getSignatorys(lv1Code, campaignCreateVo.getBudgetBelongAreaCode());
        boolean rgFlag = addSignatory(retList, signatoryRGInfos, amount);
        if (!rgFlag) {
            return retList;
        }
        // 全球
        List<SignatoryInfo> signatoryGLInfos = getSignatorys(lv1Code, "01");
        boolean glFlag = addSignatory(retList, signatoryGLInfos, amount);
        if (!glFlag) {
            return retList;
        }

        // 权签人选择业务科目lv1Code为all时，代表处层级
        String subjectCode = CommonConstant.ALL;
        List<SignatoryInfo> signatoryAllROInfos = getSignatorys(subjectCode, campaignCreateVo.getRepresentative());
        boolean allRoFlag = addSignatory(retList, signatoryAllROInfos, amount);
        if (!allRoFlag) {
            return retList;
        }
        // 地区部
        List<SignatoryInfo> signatoryAllRGInfos = getSignatorys(subjectCode, campaignCreateVo.getBudgetBelongAreaCode());
        boolean allRgFlag = addSignatory(retList, signatoryAllRGInfos, amount);
        if (!allRgFlag) {
            return retList;
        }
        // 全球
        List<SignatoryInfo> signatoryAllGLInfos = getSignatorys(subjectCode, "01");
        boolean allGlFlag = addSignatory(retList, signatoryAllGLInfos, amount);
        if (!allGlFlag) {
            return retList;
        } else {
            ActivityType activityType = getName(lv1Code);
            AreaEntity entity = getAreaName(campaignCreateVo.getRepresentative());
            if (retList.size() == 0) {
                if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang()) || CommonConstant.EN_US_LANGUAGE.equals(RequestUtil.getLang())) {
                    throw new RRException(String.format(I18nUtil.getMessage("NoQueryApprovers"), entity.getNameEn(), activityType.getNameEn()));
                } else {
                    throw new RRException(String.format(I18nUtil.getMessage("NoQueryApprovers"), entity.getNameCn(), activityType.getNameCn()));
                }
            }
            if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang()) || CommonConstant.EN_US_LANGUAGE.equals(RequestUtil.getLang())) {
                throw new RRException(String.format(I18nUtil.getMessage("NoOneApprovesTheMaximumAmount"), entity.getNameEn(), activityType.getNameEn()));
            } else {
                throw new RRException(String.format(I18nUtil.getMessage("NoOneApprovesTheMaximumAmount"), entity.getNameCn(), activityType.getNameCn()));
            }
        }
    }

    public ActivityType getName(String lv1Code) {
        Wrapper<ActivityType> typeWrapper = new EntityWrapper<>();
        typeWrapper.eq("code", lv1Code);
        typeWrapper.eq("status", "1");
        ActivityType activityType = activityTypeService.selectOne(typeWrapper);
        return activityType;
    }

    public AreaEntity getAreaName(String areaCode) {
        Wrapper<AreaEntity> areaWrapper = new EntityWrapper<>();
        areaWrapper.eq("code", areaCode);
        AreaEntity entity = areaService.selectOne(areaWrapper);
        return entity;
    }

    public boolean addSignatory(List<SignatoryInfo> retList, List<SignatoryInfo> signatoryInfos, BigDecimal amount) {
        if(ObjectUtil.isNotEmpty(retList)){
            SignatoryInfo info = retList.get(retList.size() - 1);
            for(SignatoryInfo info1:signatoryInfos){
                if(info1.getMaxAmount().compareTo(info.getMaxAmount())< 0){
                 continue;
                }else if(info1.getMaxAmount().compareTo(info.getMaxAmount()) >= 0 && info1.getMaxAmount().compareTo(amount) < 0){
                    retList.add(info1);
                } else {
                    retList.add(info1);
                    return false;
                }
            }
        }else{
            for (SignatoryInfo info : signatoryInfos) {
                if (info.getMaxAmount().compareTo(amount) < 0) {
                    retList.add(info);
                } else {
                    retList.add(info);
                    return false;
                }
            }
        }
        return true;
    }

    public List<SignatoryInfo> getSignatorys(String lv1Code, String areaCode) {
        Wrapper<SignatoryInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("lv1_subject", lv1Code);
        wrapper.eq("area_code", areaCode);
        wrapper.orderBy("max_amount", true);
        List<SignatoryInfo> signatoryInfos = signatoryInfoService.selectList(wrapper);
        return signatoryInfos;
    }

    /**
     * 和上一个版本比金额，相同则true，不同或者不存在则为false，需要重新获取权签人
     *
     * @param map
     * @param mapHis
     * @return
     */
    public Map<String, Boolean> compareHis(Map<String, BigDecimal> map, Map<String, BigDecimal> mapHis) {
        Map<String, Boolean> retMap = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : map.entrySet()) {
            if (mapCheck(mapHis, entry)) {
                retMap.put(entry.getKey(), true);
            } else {
                retMap.put(entry.getKey(), false);
            }
        }
        return retMap;
    }

    public boolean mapCheck(Map<String, BigDecimal> mapHis, Map.Entry<String, BigDecimal> entry) {
        if (mapHis.containsKey(entry.getKey()) && mapHis.get(entry.getKey()).compareTo(entry.getValue()) == 0) {
            return true;
        }
        return false;
    }

    public Map<String, BigDecimal> getLv2SubjectAmount(List<ActivityHistoryInfoVo> activityInfos) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (ActivityHistoryInfoVo info : activityInfos) {
            Map<String, Object> param = new HashMap<>();
            param.put("fromCurrency", info.getCurrency());
            BigDecimal bigDecimal = exchangeService.queryExchange(param);
            if (map.containsKey(info.getLv1Subject())) {
                map.put(info.getLv1Subject(), map.get(info.getLv1Subject()).add(info.getAmount().multiply(bigDecimal)));
            } else {
                map.put(info.getLv1Subject(), info.getAmount().multiply(bigDecimal));
            }
        }
        return map;
    }


    public Map<String, Object> getSignatoryList(CampaignCreateVo campaignCreateVo) {
        Map<String, Object> retMap = new HashMap<>();
        List<List<Long>> userIdList = new ArrayList<>();
        List<ActivityHistoryInfoVo> activityInfos = campaignCreateVo.getActivityInfos();
        if (null == activityInfos || activityInfos.size() < 1) {
            retMap.put("msg", "The submitted campaign is missing activity Lv2 information,please edit and resubmit ");
            return retMap;
        }
        //将业务科目LV1的activity的金额累加
        Map<String, BigDecimal> map = new HashMap<>();
        for (ActivityHistoryInfoVo info : activityInfos) {
            Map<String, Object> param = new HashMap<>();
            param.put("fromCurrency", info.getCurrency());
            BigDecimal bigDecimal = exchangeService.queryExchange(param);
            if (map.containsKey(info.getLv1Subject())) {
                map.put(info.getLv1Subject(), map.get(info.getLv1Subject()).add(info.getAmount().multiply(bigDecimal)));
            } else {
                map.put(info.getLv1Subject(), info.getAmount().multiply(bigDecimal));
            }
        }
        List<String> areaList = new ArrayList<>();
        areaList.add(campaignCreateVo.getBudgetBelongAreaCode());
        areaList.add(campaignCreateVo.getRepresentative());
        areaList.add("01");// 全球
//        List<String> subjectList = new ArrayList<>();
//        subjectList.add("MKT-1");
        for (String subject : map.keySet()) {
            SignatoryInfo signatoryInfo = new SignatoryInfo();
            signatoryInfo.setAreaList(areaList);
//            subjectList.add(subject);
//            signatoryInfo.setSubjectList(subjectList);
            signatoryInfo.setLv1Subject(subject);
            signatoryInfo.setMaxAmount(map.get(subject));
            List<SignatoryInfoVo> signatoryInfoVos = campaignDao.selectApproveMinAmount(signatoryInfo);
            SignatoryInfoVo minVo = null;
            if (null != signatoryInfoVos && signatoryInfoVos.size() > 0) {
                Map<String, SignatoryInfoVo> signatoryInfoVoMap = signatoryInfoVos.stream().collect(Collectors.toMap(SignatoryInfoVo::getAreaCode, (p) -> p));
                if (signatoryInfoVoMap.containsKey(campaignCreateVo.getRepresentative())) {
                    minVo = signatoryInfoVoMap.get(campaignCreateVo.getRepresentative());
                    List<Long> signatoryIdList = this.getSignatoryList(minVo);
                    userIdList.add(signatoryIdList);
                    continue;
                }
                if (null == minVo && signatoryInfoVoMap.containsKey(campaignCreateVo.getBudgetBelongAreaCode())) {
                    minVo = signatoryInfoVoMap.get(campaignCreateVo.getBudgetBelongAreaCode());
                    List<Long> signatoryIdList = this.getSignatoryList(minVo);
//                   userIdList.addAll(signatoryIdList);
                }
                if (null == minVo && signatoryInfoVoMap.containsKey("01")) {
                    minVo = signatoryInfoVoMap.get("01");
                }
            } else {
                signatoryInfo.setLv1Subject("MKT-1"); //所有科目code为all
                List<SignatoryInfoVo> signatoryInfoVoAlls = campaignDao.selectApproveMinAmount(signatoryInfo);
                if (null != signatoryInfoVoAlls && signatoryInfoVoAlls.size() > 0) {
                    Map<String, SignatoryInfoVo> signatoryInfoVoMapAll = signatoryInfoVoAlls.stream().collect(Collectors.toMap(SignatoryInfoVo::getAreaCode, (p) -> p));
                    if (signatoryInfoVoMapAll.containsKey(campaignCreateVo.getRepresentative())) {
                        minVo = signatoryInfoVoMapAll.get(campaignCreateVo.getRepresentative());
                    }
                    if (null == minVo && signatoryInfoVoMapAll.containsKey(campaignCreateVo.getBudgetBelongAreaCode())) {
                        minVo = signatoryInfoVoMapAll.get(campaignCreateVo.getBudgetBelongAreaCode());
                    }
                    if (null == minVo && signatoryInfoVoMapAll.containsKey("01")) {
                        minVo = signatoryInfoVoMapAll.get("01");
                    }
                } else {
                    Map<String, AreaEntity> areaMap = new HashMap<>();
                    Map<String, ActivityType> activityMap = new HashMap<>();
                    areaMap = getAreaMap(areaMap);
                    activityMap = getActivityTypesMap(activityMap);
                    if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                        retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSignatory"),
                                areaMap.get(campaignCreateVo.getRepresentative()).getNameEn(), activityMap.get(subject).getNameEn()));
                    } else {
                        retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSignatory"),
                                areaMap.get(campaignCreateVo.getRepresentative()).getNameCn(), activityMap.get(subject).getNameCn()));
                    }
                    return retMap;
                }
            }
            if (null != minVo) {
                // 反查userId
                Wrapper<SignatoryInfo> wrapper = new EntityWrapper<>();
                wrapper.eq("lv1_subject", minVo.getLv1Subject());
                wrapper.eq("area_code", minVo.getAreaCode());
                wrapper.le("max_amount", minVo.getMaxAmount());
                wrapper.orderBy("max_amount", true);
//               wrapper.eq("max_amount", minVo.getMaxAmount());
                List<SignatoryInfo> signatoryInfos = signatoryInfoService.selectList(wrapper);
//               userIdList.add(Long.parseLong(info.getUserId()));
            }
        }
        retMap.put("msg", "success");
        retMap.put("data", userIdList);
        return retMap;
    }


    public List<Long> getSignatoryList(SignatoryInfoVo minVo) {
        Wrapper<SignatoryInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("lv1_subject", minVo.getLv1Subject());
        wrapper.eq("area_code", minVo.getAreaCode());
        wrapper.le("max_amount", minVo.getMaxAmount());
        wrapper.orderBy("max_amount", true);
        List<SignatoryInfo> signatoryInfos = signatoryInfoService.selectList(wrapper);
        List<Long> distinct = signatoryInfos.stream().map(vo -> Long.parseLong(vo.getUserId())).distinct().collect(Collectors.toList());
        return distinct;
    }

    @Override
    @Transactional
    public R save(CampaignCreateVo campaignCreateVo) {
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        String operator = SecurityUtils.getSubject().getPrincipal() == null ? null : ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId().toString();
        if (operator == null) {
            return R.error(401, isChinese ? TipEnum.NOTLOGIN.getStatusNameCn() : TipEnum.NOTLOGIN.getStatusNameEn());
        }

        // 校验用户是否具备campaignOwner或者PA角色
        List<String> checkRoleNames = Arrays.asList(RoleNameEnum.CAMPAIGN_PA.getRoleNameEn(), RoleNameEnum.CAMPAIGN_OWNER.getRoleNameEn());
        boolean roleCheckRes = campaignCommonService.checkSysUserRole(Long.parseLong(operator), checkRoleNames);
        if (!roleCheckRes) {
            return R.error(I18nUtil.getMessage("NotCampaignOwnerOrPaRole"));
        }

        // 驳回非当前处理人不能提交和保存
        String checkMsg = checkIsApplyUser(campaignCreateVo, operator);
        if (StringUtils.isNotEmpty(checkMsg)) {
            return R.error(checkMsg);
        }

//        campaignCreateVo.setYear("" + Calendar.getInstance().get(Calendar.YEAR));
        campaignCreateVo.setSubmitDate(new Date());
        String campaignSaveId = campaignCreateVo.getCampaignSaveId();
        String campaignId = Strings.isEmpty(campaignSaveId) && Strings.isEmpty(campaignCreateVo.getCampaignId()) ? getCampaignId().toString() : Strings.isNotEmpty(campaignSaveId) ? campaignSaveId : campaignCreateVo.getCampaignId();
        campaignCreateVo.setCampaignId(campaignId);

        // 1. 拷贝、查询
        setCompaignVersion(campaignCreateVo);
        boolean isAddRecord = insertOrUpdateCampaignHis(campaignCreateVo, operator, CommonConstant.OPERATE_TYPE_SAVE);

        // 2.保存附件信息 1:N
        saveAttachment(campaignCreateVo, operator, isAddRecord);

        // 3. 更新activity备份表信息
        R lvCheckRes1 = activityHistoryInfoService.updateInfo(campaignCreateVo, CommonConstant.OPERATE_TYPE_SAVE, isAddRecord);
        if (!("0".equals(lvCheckRes1.get("code").toString()))) {
            return lvCheckRes1;
        }

        return R.ok().put("campaignId", campaignCreateVo.getCampaignId()).put("id", campaignCreateVo.getId());
    }

    private void saveAttachment(CampaignCreateVo campaignCreateVo, String operator, boolean isAddRecord) {
        // 1.先删除数据库中不存在的
        Wrapper<AttachmentInfo> attachWrapper = new EntityWrapper<>();
        attachWrapper.eq("ref_id", campaignCreateVo.getId());
        attachWrapper.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<AttachmentInfo> dbAttachmentInfoVos = attachmentInfoService.selectList(attachWrapper);
        if (CollectionUtils.isNotEmpty(dbAttachmentInfoVos)) {
            // 数据库中原来保存过此campaign附件,先删除库中不在本次上传中的
            dbAttachmentInfoVos.stream()
                    .filter(item -> !campaignCreateVo.getAttachmentInfos().stream()
                            .map(AttachmentInfo::getAttachmentId)
                            .collect(Collectors.toList())
                            .contains(item.getAttachmentId()))
                    .collect(Collectors.toList()).stream().forEach(at -> {
                        at.setStatus(MyConstant.DEL_STATUS_DELETED);
                        Wrapper<AttachmentInfo> atWrapper = new EntityWrapper<>();
                        atWrapper.eq("attachment_id", at.getAttachmentId());
                        attachmentInfoService.update(at, atWrapper);
                    });
        }

        // 2.保存or更新此次上传的
        List<AttachmentInfo> attachmentInfos = campaignCreateVo.getAttachmentInfos();
        if (CollectionUtils.isEmpty(attachmentInfos)) {
            return;
        }
        attachmentInfos.stream().forEach(attachmentInfo -> {
            if (isAddRecord) {
                // 非便捷场景， 新增记录
                attachmentInfo.setAttachmentId(null);
            }
            attachmentInfo.setRefId(campaignCreateVo.getId().toString());
            attachmentInfo.setCreatedBy(operator);
            attachmentInfo.setStatus(MyConstant.DEL_STAUTS_NORMAL);
            attachmentInfo.setCreatedDate(new Date());
        });
        attachmentInfoService.insertOrUpdateBatch(attachmentInfos);
    }

    private void updateCampaignTableHis(String operator, CampaignCreateVo campaignCreateVo, String operateType) {
        String status = getNextCampaignStatusCode(campaignCreateVo, operateType);
        Date date = new Date();

        CampaignHistoryInfoVo campaignHistoryInfoVo = new CampaignHistoryInfoVo();
        BeanUtils.copyProperties(campaignCreateVo, campaignHistoryInfoVo);
        campaignHistoryInfoVo.setStatus(status);
        campaignHistoryInfoVo.setUpdatedDate(date);
        campaignHistoryInfoVo.setUpdatedBy(operator);
        campaignHistoryInfoService.updateById(campaignHistoryInfoVo);

        // 入参中没有processId， 反查下
        CampaignHistoryInfoVo dbVo = campaignHistoryInfoService.selectById(campaignCreateVo.getId());
        log.info("updateCampaignTableHis processId:", dbVo.getProcessId());
        campaignCreateVo.setProcessId(dbVo.getProcessId());
        campaignCreateVo.setStatus(status);
    }

    /**
     * 根据操作判断campaign下一步状态
     *
     * @param campaignCreateVo
     * @param operateType
     * @return
     */
    private String getNextCampaignStatusCode (CampaignCreateVo campaignCreateVo, String operateType) {
        if (CommonConstant.OPERATE_TYPE_SUBMIT.equals(operateType)){
            // 提交操作， 下一步状态都是评审中
            return CampaignStatusEnum.WAITING_REVIEW.getStatusCode();
        }
        // 保存操作
        if (null == campaignCreateVo.getId()) {
            return CampaignStatusEnum.DRAFT.getStatusCode();
        }

        CampaignHistoryInfoVo info = campaignHistoryInfoService.selectById(campaignCreateVo.getId());
        if (campaignCommonService.isReturnStatus(info.getStatus()) ) {
            // 驳回后保存
            return CampaignStatusEnum.RETURNED.getStatusCode();
        }

        return CampaignStatusEnum.DRAFT.getStatusCode();
    }

    private void insertCampaignTableHis(String operator, CampaignCreateVo campaignCreateVo, String operateType) {
        Date date = new Date();
        String status = getNextCampaignStatusCode(campaignCreateVo, operateType);

        // 保存到历史表
        CampaignHistoryInfoVo campaignHistoryInfoVo = new CampaignHistoryInfoVo();
        BeanUtils.copyProperties(campaignCreateVo, campaignHistoryInfoVo);
        campaignHistoryInfoVo.setCampaignId(campaignCreateVo.getCampaignId());
        campaignHistoryInfoVo.setStatus(status);
        campaignHistoryInfoVo.setIsValidVersion(CommonConstant.VALID_VERISON_EN);
        campaignHistoryInfoVo.setCreatedDate(date);
        campaignHistoryInfoVo.setCreatedBy(operator);
        campaignHistoryInfoVo.setUpdatedDate(date);
        campaignHistoryInfoVo.setUpdatedBy(operator);
        campaignHistoryInfoService.insert(campaignHistoryInfoVo);

        campaignCreateVo.setId(campaignHistoryInfoVo.getId());
        campaignCreateVo.setProcessId(null);
        campaignCreateVo.setStatus(campaignHistoryInfoVo.getStatus());
    }


    /**
     * 与历史数据比较: 如果相同返回true,不同返回false
     *
     * @param activityInfos
     * @param campaignCreateVo
     * @return
     */
    private boolean compareLv2(List<ActivityHistoryInfoVo> activityInfos, CampaignCreateVo campaignCreateVo) {
        List<ActivityHistoryInfoVo> voActivityInfos = campaignCreateVo.getActivityInfos();
        // 1.判断是否有新增L2
        int hosierySize = activityInfos.size();
        int newSize = voActivityInfos.size();
        if (hosierySize != newSize) {
            return false;
        }

        // 2.无新增则对比数据有无更新
        for (int i = 0; i < hosierySize; i++) {
            ActivityHistoryInfoVo hosieryActicity = activityInfos.get(i);
            ActivityHistoryInfoVo newActivity = voActivityInfos.get(i);
            // 金额
            if (hosieryActicity.getAmount().compareTo(newActivity.getAmount()) != 0) {
                return false;
            }
            // 活动描述
            if (!hosieryActicity.getDescription().equals(newActivity.getDescription())) {
                return false;
            }
            // 科目一
            if (!hosieryActicity.getLv1Subject().equals(newActivity.getLv1Subject())) {
                return false;
            }
            // 科目二
            if (!hosieryActicity.getLv2Subject().equals(newActivity.getLv2Subject())) {
                return false;
            }
            // 币种
            if (!hosieryActicity.getCurrency().equals(newActivity.getCurrency())) {
                return false;
            }
            // 是否BP规划内
//            if (!hosieryActicity.getIsInBp().equals(newActivity.getIsInBp())){
//                return false;
//            }
            // 活动开始时间
            if (hosieryActicity.getStartDate().compareTo(newActivity.getStartDate()) != 0) {
                return false;
            }
            // 活动结束时间
            if (hosieryActicity.getEndDate().compareTo(newActivity.getEndDate()) != 0) {
                return false;
            }
        }
        //遍历后都相同，返回true,不需要权签人
        return true;
    }


    /**
     * 查询是否存在业务配置人员
     *
     * @param campaignHistoryInfoVo
     * @return
     */
    private Map<String, Object> getApproverInfos(CampaignHistoryInfoVo campaignHistoryInfoVo) {
        Map<String, AreaEntity> areaMap = new HashMap<>();
        Map<String, ActivityType> activityMap = new HashMap<>();
        Map<String, Object> retMap = new HashMap<>();
        ApproverInfo approverInfo = new ApproverInfo();
        List<String> areaList = new ArrayList<>();
        String representiveCode = campaignHistoryInfoVo.getRepresentative();
        String regionCode = campaignHistoryInfoVo.getBudgetBelongAreaCode();
        areaList.add(regionCode);
        areaList.add(representiveCode);
        areaList.add("01");
        approverInfo.setAreaList(areaList);
        List<ActivityHistoryInfoVo> activityInfos = campaignHistoryInfoVo.getActivityInfos();
        if (null == activityInfos || activityInfos.size() < 1) {
            retMap.put("msg", "The submitted campaign is missing activity Lv2 information,please edit and resubmit ");
            return retMap;
        }
        List<ApproverInfoVo> approverInfos = new ArrayList<>();
        for (ActivityHistoryInfoVo info : activityInfos) {
            String lv2Subject = info.getLv2Subject();
            String lv1Subject = info.getLv1Subject();
            String approvalSubjectCode = null;
            List<String> subjectList = new ArrayList<>();
            subjectList.add(lv2Subject);
            if (("MKT1").equals(lv1Subject)) {
                subjectList.add("MKT-1"); //所有科目code为all
                approvalSubjectCode = "MKT-1";
            } else {
                subjectList.add("MKT-2");
                approvalSubjectCode = "MKT-1";
            }
            subjectList.add("MKT0");
            approverInfo.setSubjectList(subjectList);
            approverInfo.setSource(info.getSource());
            List<ApproverInfoVo> approverInfoVos = approverInfoService.queryData(approverInfo);
            if (null == approverInfoVos || approverInfoVos.size() < 1) {
                log.info("科目：" + lv2Subject + ",区域：" + campaignHistoryInfoVo.getRepresentative() + ",来源：" + info.getSource());
                areaMap = getAreaMap(areaMap);
                activityMap = getActivityTypesMap(activityMap);
//                retMap.put("msg","区域："+areaMap.get(campaignCreateVo.getRepresentative())
//                        +"科目:"+activityMap.get(lv2Subject).getNameCn()
//                        +"来源："+info.getSource() +I18nUtil.getMessage("NoConfigSalesperson"));
                if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                    retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSalesperson"),
                            areaMap.get(campaignHistoryInfoVo.getRepresentative()).getNameEn(),
                            activityMap.get(lv2Subject).getNameEn(), info.getSource()));
                } else {
                    retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSalesperson"),
                            areaMap.get(campaignHistoryInfoVo.getRepresentative()).getNameCn(),
                            activityMap.get(lv2Subject).getNameCn(), info.getSource()));
                }
                return retMap;
            } else {
                List<ApproverInfoVo> approverInfoVos1 = new ArrayList<>(); // lv2Code业务专家
                List<ApproverInfoVo> approverInfoVos2 = new ArrayList<>(); //
                List<ApproverInfoVo> approverInfoVos3 = new ArrayList<>(); // mkt0业务专家
                for (ApproverInfoVo vo : approverInfoVos) {
                    if (vo.getLv2Subject().equals(lv2Subject)) {
                        approverInfoVos1.add(vo);
                    } else if (vo.getLv2Subject().equals("MKT0")) {
                        approverInfoVos2.add(vo);
                    } else {
                        approverInfoVos3.add(vo);
                    }
                }
                if (approverInfoVos1.size() != 0) {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos1, representiveCode, regionCode);
                    approverInfos.add(minVo);
                } else if (approverInfoVos3.size() != 0) {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos3, representiveCode, regionCode);
                    approverInfos.add(minVo);
                } else {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos2, representiveCode, regionCode);
                    approverInfos.add(minVo);
                }


                List<ApproverInfoVo> globalApprovers = new ArrayList<>();
                List<ApproverInfoVo> regionApprovers = new ArrayList<>();
                List<ApproverInfoVo> representativeApprovers = new ArrayList<>();
                for(ApproverInfoVo vo : approverInfoVos){
                    if(representiveCode.equals(vo.getAreaCode())){
                        representativeApprovers.add(vo);
                    }else if(regionCode.equals(vo.getAreaCode())){
                        regionApprovers.add(vo);
                    } else {
                        globalApprovers.add(vo);
                    }
                }
                ApproverInfoVo retVO = null;
                if(representativeApprovers.size() !=0){
                    // 按照业务科目2层级获取业务专家
                    retVO= getApproverBysubject2(representativeApprovers, lv2Subject, approvalSubjectCode);
                } else if(regionApprovers.size() !=0){
                    retVO = getApproverBysubject2(regionApprovers, lv2Subject, approvalSubjectCode);
                } else {
                    retVO = getApproverBysubject2(globalApprovers, lv2Subject, approvalSubjectCode);
                }
                approverInfos.add(retVO);
            }
        }
        retMap.put("msg", "success");
        retMap.put("data", approverInfos);
        return retMap;
    }

    /**
     * 查询是否存在业务配置人员
     *
     * @param campaignCreateVo
     * @return
     */
    private Map<String, Object> getApproverInfos(CampaignCreateVo campaignCreateVo) {
        String representiveCode = campaignCreateVo.getRepresentative();
        String regionCode = campaignCreateVo.getBudgetBelongAreaCode();
        Map<String, AreaEntity> areaMap = new HashMap<>();
        Map<String, ActivityType> activityMap = new HashMap<>();
        Map<String, Object> retMap = new HashMap<>();
        ApproverInfo approverInfo = new ApproverInfo();
        List<String> areaList = new ArrayList<>();
        areaList.add(regionCode);
        areaList.add(representiveCode);
        areaList.add("01");
        approverInfo.setAreaList(areaList);
        List<ActivityHistoryInfoVo> activityInfos = campaignCreateVo.getActivityInfos();
        if (null == activityInfos || activityInfos.size() < 1) {
            retMap.put("msg", "The submitted campaign is missing activity Lv2 information,please edit and resubmit ");
            return retMap;
        }
        List<ApproverInfoVo> approverInfos = new ArrayList<>();
        for (ActivityHistoryInfoVo info : activityInfos) {
            String lv2Subject = info.getLv2Subject();
            String lv1Subject = info.getLv1Subject();
            List<String> subjectList = new ArrayList<>();
            subjectList.add(lv2Subject);
            String approvalSubjectCode = null;
            if (("MKT1").equals(lv1Subject)) {
                subjectList.add("MKT-1"); //所有科目code为all
                approvalSubjectCode = "MKT-1";
            } else {
                subjectList.add("MKT-2");
                approvalSubjectCode = "MKT-2";
            }
            subjectList.add("MKT0");
            approverInfo.setSubjectList(subjectList);
            approverInfo.setSource(info.getSource());
            List<ApproverInfoVo> approverInfoVos = approverInfoService.queryData(approverInfo);
            if (null == approverInfoVos || approverInfoVos.size() < 1) {
                log.info("科目：" + lv2Subject + ",区域：" + campaignCreateVo.getRepresentative() + ",来源：" + info.getSource());
                areaMap = getAreaMap(areaMap);
                activityMap = getActivityTypesMap(activityMap);
//                retMap.put("msg","区域："+areaMap.get(campaignCreateVo.getRepresentative())
//                        +"科目:"+activityMap.get(lv2Subject).getNameCn()
//                        +"来源："+info.getSource() +I18nUtil.getMessage("NoConfigSalesperson"));
                if (CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())) {
                    retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSalesperson"),
                            areaMap.get(campaignCreateVo.getRepresentative()).getNameEn(),
                            activityMap.get(lv2Subject).getNameEn(), info.getSource()));
                } else {
                    retMap.put("msg", String.format(I18nUtil.getMessage("NoConfigSalesperson"),
                            areaMap.get(campaignCreateVo.getRepresentative()).getNameCn(),
                            activityMap.get(lv2Subject).getNameCn(), info.getSource()));
                }
                return retMap;
            } else {
                List<ApproverInfoVo> approverInfoVos1 = new ArrayList<>(); // lv2Code业务专家
                List<ApproverInfoVo> approverInfoVos2 = new ArrayList<>(); // mkt0(ALL)业务专家
                List<ApproverInfoVo> approverInfoVos3 = new ArrayList<>(); // mkt-1(Marketing-ALL)或者mkt-2(零售-ALL)业务专家
                for (ApproverInfoVo vo : approverInfoVos) {
                    if (vo.getLv2Subject().equals(lv2Subject)) {
                        approverInfoVos1.add(vo);
                    } else if (vo.getLv2Subject().equals("MKT0")) {
                        approverInfoVos2.add(vo);
                    } else {
                        approverInfoVos3.add(vo);
                    }
                }
                //按地域级别获取业务专家
                if (approverInfoVos1.size() != 0) {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos1, representiveCode,regionCode);
                    approverInfos.add(minVo);
                } else if (approverInfoVos3.size() != 0) {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos3, representiveCode,regionCode);
                    approverInfos.add(minVo);
                } else {
                    ApproverInfoVo minVo = this.getMinVo(approverInfoVos2, representiveCode,regionCode);
                    approverInfos.add(minVo);
                }
            }
        }
        retMap.put("msg", "success");
        retMap.put("data", approverInfos);
        return retMap;
    }

    public ApproverInfoVo getApproverBysubject2(List<ApproverInfoVo> approverInfoVos, String lv2Subject, String approvalSubjectCode){
        Map<String, ApproverInfoVo> collect = approverInfoVos.stream().collect(Collectors.toMap(ApproverInfoVo::getLv2Subject, p -> p));
        if(null != collect.get(lv2Subject)){
            return collect.get(lv2Subject);
        } else if(null != collect.get(approvalSubjectCode)){
            return collect.get(approvalSubjectCode);
        }else{
            return collect.get("MKT0");
        }
    }

    public Map<String, AreaEntity> getAreaMap(Map<String, AreaEntity> areaMap) {
        if (areaMap.isEmpty()) {
            List<AreaEntity> areaEntities = areaService.selectList(null);
            areaEntities.forEach(area -> areaMap.put(area.getCode(), area));
        }
        return areaMap;
    }

    public Map<String, ActivityType> getActivityTypesMap(Map<String, ActivityType> activityTypeMap) {
        if (activityTypeMap.isEmpty()) {
            List<ActivityType> typesList = activityTypeService.selectList(null);
            typesList.forEach(type -> activityTypeMap.put(type.getCode(), type));
        }
        return activityTypeMap;
    }

    public ApproverInfoVo getMinVo(List<ApproverInfoVo> approverInfoVos, String representative,String rudgetBelongAreaCode ) {
        Map<String, ApproverInfoVo> approverInfoVoMap = approverInfoVos.stream().collect(Collectors.toMap(ApproverInfoVo::getAreaCode, p -> p));
        ApproverInfoVo minVo = null;
        if (approverInfoVoMap.containsKey(representative)) {
            minVo = approverInfoVoMap.get(representative);
        }
        if (null == minVo && approverInfoVoMap.containsKey(rudgetBelongAreaCode)) {
            minVo = approverInfoVoMap.get(rudgetBelongAreaCode);
        }
        if (null == minVo && approverInfoVoMap.containsKey("01")) {
            minVo = approverInfoVoMap.get("01");
        }
        return minVo;
    }

    /**
     * campaign参数校验
     *
     * @param campaignCreateVo
     * @return
     */
    private String checkParam(CampaignCreateVo campaignCreateVo, boolean isChinese) {
        String errMsg = null;
        if (null == campaignCreateVo) {
            errMsg = isChinese ? TipEnum.PARAM_EMPTY.getStatusNameCn() : TipEnum.PARAM_EMPTY.getStatusNameEn();
            return errMsg;
        }
        if (CollectionUtils.isEmpty(campaignCreateVo.getActivityInfos())) {
            errMsg = isChinese ? TipEnum.LV2_EMPTY.getStatusNameCn() : TipEnum.LV2_EMPTY.getStatusNameEn();
            return errMsg;
        }
        return null;
    }

    @Override
    public List<String> getActivityPidByBudgetCode(String budgetCode) {
        List<String> activitis = campaignDao.getActivityPidByBudgetCode(budgetCode);
        return activitis;
    }

    @Override
    public CampaignHistoryInfoVo copyCampaign(String campaignId, Integer id) {
        CampaignHistoryInfoVo campaignCreateVo = campaignHistoryInfoService.getCampaignHistoryDetail(campaignId, id);

        // 去除id
        campaignCreateVo.setId(null);
        campaignCreateVo.setCampaignId(null);
        campaignCreateVo.setProcessId(null);
        campaignCreateVo.setYear(null);
        campaignCreateVo.setSubmitDate(null);
        campaignCreateVo.setStatus(null);
        campaignCreateVo.setCreatedBy(null);
        campaignCreateVo.setCreatedDate(null);
        // 附件信息不用复制
        campaignCreateVo.setAttachmentInfos(new ArrayList<>());
        campaignCreateVo.setAttachmentsName(null);

        // 取所有币种的最新汇率
        List<ExchangeRateInfoVo> rates = exchangeService.getLastestRateList();

        if (campaignCreateVo.getActivityInfos() != null) {
            campaignCreateVo.getActivityInfos().stream().forEach(a -> {
                a.setId(null);
                a.setActivityId(null);
                a.setProcessId(null);
                a.setStatus(null);
                a.setParentId(null);
                a.setAttachments(new ArrayList<>());
                a.setAttachmentsName(null);
                a.setCreatedBy(null);
                a.setCreatedTime(null);
                a.setRecoveryAmountUsd(null);
                a.setActuallyUsedAmountUsd(null);
                a.setIsCanEditLv2Subject(true);
                a.setIsCanEditApplyAmount(true);
                a.setIsShowDeleteBtn(true);
                a.setIsShowEditBtn(true);
                // 复制场景初始汇率和最新汇率都取最新值
                BigDecimal rate = campaignHistoryInfoService.getLatestRate(a.getCurrency(), rates);
                a.setInitRate(rate);
                a.setLatestRate(rate);

                List<ActivityHistoryInfoVo> activityL3s = a.getActivityL3s();
                if (activityL3s != null) {
                    activityL3s.stream().forEach(a3 -> {
                        a3.setId(null);
                        a3.setActivityId(null);
                        a3.setProcessId(null);
                        a3.setStatus(null);
                        a3.setParentId(null);
                        a3.setActuallyUsedAmountUsd(null);
                        a3.setRecoveryAmountUsd(null);
                        a3.setCreatedBy(null);
                        a3.setCreatedTime(null);
                        a3.setRefId(null);
                        a3.setPrId(null);

                        PurchaseInfo purchaseInfo = a3.getPurchaseInfo();
                        purchaseInfo.setUuid(null);
                        purchaseInfo.setRefId(null);
                        purchaseInfo.setCreatedBy(null);
                        purchaseInfo.setCreatedTime(null);
                        a3.setPurchaseInfo(purchaseInfo);

                        FinanceInfo financeInfo = a3.getFinanceInfo();
                        financeInfo.setUuid(null);
                        financeInfo.setRefId(null);
                        financeInfo.setCreatedTime(null);
                        financeInfo.setCreatedBy(null);
                        a3.setFinanceInfo(financeInfo);
                        BigDecimal rate1 = campaignHistoryInfoService.getLatestRate(financeInfo.getCurrency(), rates);
                        a3.setInitRate(rate1);  // 复制场景初始汇率和最新汇率都取最新值
                        a3.setLatestRate(rate1);

                        a3.setIsCanEditLv3Subject(true);
                        a3.setIsCanEditPurchaseSubject(true);
                        a3.setIsCanEditApplyAmount(true);
                        a3.setIsShowDeleteBtn(true);
                        a3.setIsShowEditBtn(true);

                        if (a3.getProducts() != null) {
                            a3.getProducts().stream().forEach(p -> {
                                p.setUuid(null);
                                p.setRefId(null);
                                p.setCreatedBy(null);
                                p.setCreatedDate(null);
                            });
                        }
                    });
                }
            });
        }
        return campaignCreateVo;
    }

    /**
     * 通过campaignId 删除campaign
     *
     * @param campaignId
     * @return
     */
    @Override
    public R deleteByCampaignId(String campaignId, Integer id) {
        if (null == id) {
            return R.error(500, "id is null");
        }

        CampaignHistoryInfoVo campaignHistoryInfoVo = campaignHistoryInfoService.selectById(id);

        if (!CampaignStatusEnum.DRAFT.getStatusCode().equals(campaignHistoryInfoVo.getStatus())) {
            return R.error(500, I18nUtil.getMessage("ProcessIsApprovalingCanNotDelete"));
        }

        campaignHistoryInfoVo.setStatus(CampaignStatusEnum.DELETE.getStatusCode());
        campaignHistoryInfoVo.setIsValidVersion(CommonConstant.INVALID_VERISON_EN);
        campaignHistoryInfoService.updateById(campaignHistoryInfoVo);

        activityHistoryInfoService.updateActivityStatusByCampaignId(campaignHistoryInfoVo.getCampaignId(), id,
                ActivityStatusEnum.CANCEL.getStatusCode(), DeleteStatusEnum.DELETED.getStatusCode());
        return R.ok();
    }


    public StringBuilder getCampaignId() {
        Wrapper<CampaignHistoryInfoVo> wrapper = new EntityWrapper<>();
        wrapper.like("campaign_id", Dates.format(new Date(), "yyyyMMdd"));
        wrapper.orderBy("campaign_id", false);
        wrapper.last("limit 1");
        CampaignHistoryInfoVo campaign = campaignHistoryInfoService.selectOne(wrapper);

        StringBuilder campaignId = new StringBuilder();
        if (campaign == null) {
            campaignId = campaignId.append("C").append(LocalDate.now().toString().replace("-", "")).append("0001");
        } else {
            campaignId = increaseActId(campaignId.append(campaign.getCampaignId()));
        }
        return campaignId;
    }

    private StringBuilder increaseActId(StringBuilder stringBuilder) {
        String num = stringBuilder.substring(9);// sb.substring(2)去掉前两个字符
        int num1 = Integer.parseInt(num);
        num1++;
        String str = String.format("%04d", num1);// 如果小于6位左边补0
        String ret = stringBuilder.substring(0, 9) + str;
        return new StringBuilder(ret);
    }

    @Override
    public R getPrDetails(Map<String, Object> map) {
        Integer page = (Integer) map.get("currentPage");
        Integer limit = (Integer) map.get("pageSize");
        map.put("page", (page - 1) * limit);
        map.put("limit", limit);
        // 相同LV3情况下查询最新的且已经审批通过的pr，按照pr时间更新时间进行排序
        List<List<?>> lists = campaignDao.queryPage(map);
        if (lists.get(0) == null) {
            return R.ok();
        }
        List<CampaignPrDetailsVO> list = (List<CampaignPrDetailsVO>) lists.get(0);
        ArrayList<CampaignPrDetailsVO> campaignPrDetailsVOS = new ArrayList<>();
        for (CampaignPrDetailsVO vo : list) {
            HashMap<String, Object> lv3Params = new HashMap<>();
            lv3Params.put("activityId", vo.getActivityIdLv3());
            lv3Params.put("lang", RequestUtil.getLang());
            CampaignPrDetailsVO prDetailsVO = campaignDao.getPrDetails(lv3Params);
            if (prDetailsVO == null) {
                log.error("getPrDetails is null" + vo.getActivityIdLv3());
                continue;
            }
            prDetailsVO.setCampaign_id(vo.getCampaign_id());
            HashMap<String, Object> param = new HashMap<>(2);
            param.put("prId", prDetailsVO.getPrId());
            param.put("activityId", prDetailsVO.getActivityIdLv3());
            // 获取po详情列表
            List<PoDetailsVO> poDetailsVoList = campaignDao.getPoDetailsVoList(param);
            poDetailsVoList.forEach(poDetailsVO -> {
                Wrapper<AcceptanceFormInfo> wrapper = new EntityWrapper<>();
                wrapper.eq("po_id", poDetailsVO.getPoId());
                List<String> status = new ArrayList<>();
                status.add("0");
                status.add("4");
                wrapper.notIn("status",status);
                List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoDao.selectList(wrapper);
                if (acceptanceFormInfos.size() > 0) {
                    BigDecimal acceptanceQty = acceptanceFormInfos
                            .stream()
                            .map(AcceptanceFormInfo :: getAcceptanceQty)
                            .reduce(BigDecimal::add)
                            .get();
                    poDetailsVO.setAcceptanceQty(acceptanceQty);
                } else {
                    poDetailsVO.setAcceptanceQty(new BigDecimal(0.000));
                }
                // 剩余验收数量
                poDetailsVO.setSurplusQty(poDetailsVO.getPoQty().subtract(poDetailsVO.getAcceptanceQty()));
            });

            prDetailsVO.setChildren(poDetailsVoList);
            campaignPrDetailsVOS.add(prDetailsVO);
        }
        lists.remove(0);
        lists.add(0, campaignPrDetailsVOS);
        PageUtils pageUtils = PageHelpUtils.getPageUtils(map, lists);
        return R.ok().put("data", pageUtils);
    }

    @Override
    public R getPoDetails(Map<String, Object> params) {
        List<PolIneDetail> poDetails = campaignDao.getPoDetails(params);
        poDetails.forEach(poDetailsVO -> {
            Wrapper<AcceptanceFormInfo> wrapper = new EntityWrapper<>();
            wrapper.eq("po_id", poDetailsVO.getPoId());
            List<String> status = new ArrayList<>();
            status.add("0");
            status.add("4");
            wrapper.notIn("status",status);
            List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoDao.selectList(wrapper);
            if (acceptanceFormInfos.size() > 0) {
                BigDecimal acceptanceQty = acceptanceFormInfos
                        .stream()
                        .map(AcceptanceFormInfo :: getAcceptanceQty)
                        .reduce(BigDecimal::add)
                        .get();
                poDetailsVO.setAcceptanceQty(acceptanceQty);
            } else {
                poDetailsVO.setAcceptanceQty(new BigDecimal(0.000));
            }
            // 剩余验收数量
            poDetailsVO.setSurplusQty(poDetailsVO.getPoLineQty().subtract(poDetailsVO.getAcceptanceQty()));
        });
        return R.ok().put("data", poDetails);
    }

    @Override
    public void getPrId(Map<String, Object> params) {
        List<PrNumberRequestVO> prNumberRequestList = campaignDao.getPrNumberRequestList(params);
        if (prNumberRequestList.isEmpty()) {
            return;
        }

        SysDictEntity qtyDefaultDict = sysDictService.getSysDist("SapPrParams", "default");
        log.info("getPrId qtySysDict:{}", qtyDefaultDict.getValue());

        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd");
        String date = simpleFormat.format(new Date());
        log.info("Get PR number from sap service begin");
        for (PrNumberRequestVO prNumberRequestVO : prNumberRequestList) {
            // 先判断是否在数据字典中有限制此Lv3， 限制了就不用走此逻辑
            SysDictEntity sceneDict = sysDictService.getSysDist("SapOrderParams", "scene1");
            if (null != sceneDict && StringUtils.isNotEmpty(sceneDict.getValue().trim()) && sceneDict.getValue().contains(prNumberRequestVO.getActivityId())){
                continue;
            }

            // 默认数量为1， 202210版本适配,先根据业务科目2的Code去匹配， 没有匹配到就取配置的默认数量
            SysDictEntity qtyDict = sysDictService.getSysDist("SapPrParams", prNumberRequestVO.getLv2SubjectCode());
            String qtyStr = (null != qtyDict && StringUtils.isNotEmpty(qtyDict.getValue()) ? qtyDict.getValue() : qtyDefaultDict.getValue());
            prNumberRequestVO.setQty(Integer.valueOf(qtyStr));
            prNumberRequestVO.setEndDate(date);
            log.info("Get PR number from sap service, request:{}", prNumberRequestVO);
            Map<String, Object> poNumber = sapRfcService.getPoNumber(prNumberRequestVO);
            log.info("Get PR number from sap service, response:{}", poNumber);
            if (!ErrorCode.SUCCESS.equals(poNumber.get("errorCode"))) {
                log.error("getPrId query sap getPoNumber fail, poNumber:{}", poNumber);
                continue;
            }

            PrInfoEntity prInfoEntity = new PrInfoEntity();
            // 只有请求的PR为空的时候，才需要插入，因为PR不为空的情况，上面的业务已经进行了更新了pr
            if (Strings.isBlank(prNumberRequestVO.getPrId())) {
                prInfoEntity.setPrId(poNumber.get("prNo").toString());
                prInfoEntity.setSupplier(prNumberRequestVO.getCompanyCode());
                prInfoEntity.setStatus(1);
                prInfoEntity.setPrStatus(PrPoStatusEnum.NORMAL.getCode());
                prInfoEntity.setRefActivityId(prNumberRequestVO.getId());
                prInfoEntity.setCreateTime(new Date());
                prInfoEntity.setUpdateTime(new Date());
                prInfoDao.insert(prInfoEntity);
            }
        }
        log.info("Get PR number from sap service end");

    }

    @Override
    public void isChangeCopyPr(String campaignId) {
        Wrapper<CampaignHistoryInfoVo> wrapper = new EntityWrapper<>();
        wrapper.eq("campaign_id", campaignId);
        List<CampaignHistoryInfoVo> campaignHistoryInfoVos = campaignHistoryInfoService.selectList(wrapper);
        if (campaignHistoryInfoVos.isEmpty() || campaignHistoryInfoVos.size() == 1) {
            return;
        }
        // 如果变更了，查询有campaign下所有的pr和lV3id
        List<PrInfoEntity> prInfoEntities = prInfoDao.queryAllPrIdByCampaign(campaignId);
       if(prInfoEntities.isEmpty()) {
            return;
        }
        // 筛选出有pr的LV3数据
        HashMap<String, PrInfoEntity> prInfoMap = new HashMap<>();
        for (PrInfoEntity prInfoEntity : prInfoEntities) {
            if (StringUtils.isNotEmpty(prInfoEntity.getPrId()) && !prInfoMap.containsKey(prInfoEntity.getActivityIdLv3())) {
                prInfoMap.put(prInfoEntity.getActivityIdLv3(), prInfoEntity);
            }
        }
        Iterator<PrInfoEntity> iterator = prInfoEntities.iterator();
        while (iterator.hasNext()) {
            PrInfoEntity prInfoEntity = iterator.next();
            if (prInfoMap.containsKey(prInfoEntity.getActivityIdLv3())) {
                if (StringUtils.isEmpty(prInfoEntity.getPrId())) {
                    // LV3有pr了
                    PrInfoEntity prInfo = prInfoMap.get(prInfoEntity.getActivityIdLv3());
                    prInfoEntity.setPrId(prInfo.getPrId());
                    prInfoEntity.setSupplier(prInfo.getSupplier());
                }
            } else {
                iterator.remove();
            }
        }
        if(prInfoEntities.isEmpty()) {
            return;
        }
        // 有pr了，将之前的置为失效
        prInfoDao.updatePrInfoByCampaign(prInfoEntities);
    }

    // 定时任务获取prId
    public void scheduleGetPrId() {
        List<Map<String, Object>> approvedCampaign = campaignDao.getApprovedCampaign();
        if (approvedCampaign.isEmpty() || approvedCampaign.size() == 0) {
            return;
        }
        for (Map<String, Object> map : approvedCampaign) {
            getPrId(map);
        }
    }

    /**
     * 查询待办转移人员列表
     * @return
     */
    public PageUtils getTransferUsers(Map<String, Object> params){
        // 查询数据字典角色名称值
        SysDictEntity sysDist = sysDictService.getSysDist(TRANSFER_ROLE, "1");
        if(sysDist != null){
            // 根据角色查询人员列表
            params.put("roleNames", Arrays.asList(sysDist.getValue().split("、")));
            return sysUserService.selectByRoleNameIn(params);
        }
        return null;
    }

    public void sendFeiShuMessageHandle4Submit(CampaignCreateVo campaignCreateVo) {
        try {
            log.info("campaign sendFeiShuMessageHandle4Submit start");
            String phone = sysConfigService.getValue(SysDictAndParamMngKeys.CAMPAIGN_REVIEW_SUBMIT_PHONE);
            if (StringUtils.isEmpty(phone))  {
                return;
            }
            String userCared = ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserCard();
            String linkUrl = String.format(campaignDetailLinkUrl, campaignCreateVo.getCampaignId(), campaignCreateVo.getId());
            StringBuffer content = new StringBuffer();
            content.append("Campaign已提交").append("（").append(campaignCreateVo.getCampaignId()).append("）").append("\\n");
            content.append("Campaign名称：").append(campaignCreateVo.getCampaignName()).append("\\n");
            content.append("提交人：").append(userCared).append("\\n");
            content.append("Campaign金额：USD ").append(campaignCreateVo.getLv2AmountUsdTotal()).append("\\n");
            content.append("更多信息：").append(linkUrl);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(()->{
                String[] split = phone.split(",");
                for (int i = 0; i < split.length; i++) {
                    try {
                        iFeiShuSendMessageService.sendMessage(split[i], content.toString());
                    } catch (Exception e) {
                        log.error("campaign sendFeiShuMessageHandle4Submit error", e);
                    }
                }
            });
            executorService.shutdown(); // 回收线程池

            log.info("campaign sendFeiShuMessageHandle4Submit end");
        } catch (Exception e) {
            log.error("campaign sendFeiShuMessageHandle4Submit exception", e);
        }
    }
}
