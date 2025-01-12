package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.common.exception.RRException;
import com.framework.common.utils.Dates;
import com.framework.common.utils.PageUtil;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysDictService;
import com.framework.modules.sys.service.SysUserService;
import com.framework.modules.sys.shiro.ShiroUtils;
import com.wiko.activiti.constant.ProcessTaskStatusEnum;
import com.wiko.activiti.dao.ProcessTaskInfoDao;
import com.wiko.activiti.vo.ProcessTaskInfoVo;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.ActivityTypeDao;
import com.wiko.emarket.dao.CampaignDao;
import com.wiko.emarket.dao.CampaignHistoryInfoDao;
import com.wiko.emarket.dao.SysUserAreaDao;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.acceptance.AcceptanceFormInfoService;
import com.wiko.emarket.service.acceptance.PrInfoService;
import com.wiko.emarket.service.campaign.*;
import com.wiko.emarket.service.emarketprocess.impl.CampaignCommonService;
import com.wiko.emarket.util.*;
import com.wiko.emarket.vo.*;
import com.wiko.emarket.vo.po.PoBillingedAmountDetailVo;
import com.wiko.emarket.vo.po.PoPaymentedAmountDetailVo;
import com.wiko.psi.util.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
@Slf4j
public class CampaignHistoryInfoServiceImpl extends ServiceImpl<CampaignHistoryInfoDao, CampaignHistoryInfoVo> implements CampaignHistoryInfoService {
    @Autowired
    private AttachmentInfoService attachmentInfoService;

    @Autowired
    private ActivityHistoryInfoService activityHisInfoService;

    @Autowired
    private SysUserAreaDao sysUserAreaDao;

    @Autowired
    private BudgetTypeService budgetTypeService;
    @Autowired
    private AreaService areaService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ProcessTaskInfoDao taskInfoDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private CampaignCommonService campaignCommonService;
    // 我的待办标识符
    private final String MY = "my";

    @Autowired
    private CampaignHistoryInfoDao campaignHistoryInfoDao;

    @Autowired
    private ActivityTypeDao activityTypeDao;

    @Autowired
    SysDictService sysDictService;

    @Autowired
    private AcceptanceFormInfoService acceptanceFormInfoService;

    @Autowired
    private PrInfoService prInfoService;

    @Autowired
    ExchangeService exchangeService;

    @Override
    public CampaignHistoryInfoVo queryCampaignHisInfo(String detailScene, String campaignId, Integer id) {
        // 当前版本
        CampaignHistoryInfoVo currentVersionCampaignDetail = getCampaignHistoryDetail(campaignId, id);

        // 高亮和上一版本的差异化字段,获取相对当前版本的上一版本campaign
        CampaignHistoryInfoVo oldVersionDetail = null;
        CampaignHistoryInfoVo previousVersionCampaign = getCampaignPreviousValidVersion(campaignId, id);
        boolean isExistPassVersion = CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(currentVersionCampaignDetail.getStatusCode());
        currentVersionCampaignDetail.setIsExistPassVersion(isExistPassVersion);
        if (null != previousVersionCampaign) {
            // 存在历史版本
            isExistPassVersion = true;
            currentVersionCampaignDetail.setIsExistPassVersion(isExistPassVersion);
            oldVersionDetail = getCampaignHistoryDetail(campaignId, previousVersionCampaign.getId());
            compareDifferent(oldVersionDetail, currentVersionCampaignDetail);
        }

        // 取所有币种的最新汇率
        List<ExchangeRateInfoVo> rates = exchangeService.getLastestRateList();

        // 组装lv2、lv3数据是否是本次新增, 是否显示删除按钮,  detailScene不为空就判断按钮
        buildLv2Lv3IsShowDeleteBtn(currentVersionCampaignDetail, detailScene, rates);
        if (StringUtils.isNotEmpty(detailScene)) {
            buildLv2Lv3IsShowDeleteBtn(currentVersionCampaignDetail, detailScene, rates);

            // campaign/L2L3 变更场景下的字段能否编辑控制
            fieldEditContrl(currentVersionCampaignDetail, oldVersionDetail);
        }

        return currentVersionCampaignDetail;
    }

    private void setLv2BeforeVersionInfo(ActivityHistoryInfoVo infoVo, CampaignHistoryInfoVo oldVersionDetail) {
        if (null == oldVersionDetail) {
            // 无历史Lv2版本
            infoVo.setInitAmount(infoVo.getAmount());
            infoVo.setInitCurrency(infoVo.getCurrency());
            infoVo.setInitLatestRate(infoVo.getLatestRate());
        } else {
            // 有历史Lv2版本
            if (CollectionUtils.isNotEmpty(oldVersionDetail.getActivityInfos())) {
                List<ActivityHistoryInfoVo> oldLvInfos = oldVersionDetail.getActivityInfos().stream().filter(item -> StringUtils.equals(item.getActivityId(), infoVo.getActivityId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(oldLvInfos)) {
                    infoVo.setInitAmount(infoVo.getAmount());
                    infoVo.setInitCurrency(infoVo.getCurrency());
                    infoVo.setInitLatestRate(infoVo.getLatestRate());
                } else {
                    infoVo.setInitAmount(oldLvInfos.get(0).getAmount());
                    infoVo.setInitCurrency(oldLvInfos.get(0).getCurrency());
                    infoVo.setInitLatestRate(oldLvInfos.get(0).getLatestRate());
                }
            }
        }
    }

    private void setLv3BeforeVersionInfo(ActivityHistoryInfoVo infoVo, CampaignHistoryInfoVo oldVersionDetail) {
        if (null == oldVersionDetail) {
            // 无历史版本
            infoVo.setInitAmount(infoVo.getFinanceInfo().getApplyAmount());
            infoVo.setInitCurrency(infoVo.getFinanceInfo().getCurrency());
            infoVo.setInitLatestRate(infoVo.getLatestRate());
        } else {
            // 有历史版本
            if (CollectionUtils.isNotEmpty(oldVersionDetail.getActivityInfos())) {
                ActivityHistoryInfoVo oldLv3Info = null;
                for (ActivityHistoryInfoVo lv2 : oldVersionDetail.getActivityInfos()) {
                    List<ActivityHistoryInfoVo> oldLv3Infos = lv2.getActivityL3s().stream().filter(item -> StringUtils.equals(item.getActivityId(), infoVo.getActivityId())).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(oldLv3Infos)) {
                        oldLv3Info = oldLv3Infos.get(0);
                        break;
                    }
                }
                if (null == oldLv3Info) {
                    infoVo.setInitAmount(infoVo.getFinanceInfo().getApplyAmount());
                    infoVo.setInitCurrency(infoVo.getFinanceInfo().getCurrency());
                    infoVo.setInitLatestRate(infoVo.getLatestRate());
                } else {
                    infoVo.setInitAmount(oldLv3Info.getFinanceInfo().getApplyAmount());
                    infoVo.setInitCurrency(oldLv3Info.getFinanceInfo().getCurrency());
                    infoVo.setInitLatestRate(oldLv3Info.getLatestRate());
                }
            }
        }
    }

    /**
     * Campaign字段可变更范围变化
     *
     * @param currentVersionCampaignDetail
     */
    private void fieldEditContrl(CampaignHistoryInfoVo currentVersionCampaignDetail, CampaignHistoryInfoVo oldVersionDetail) {
        // 1.lv3下面是否存在有效PO， 存在则不让该LV3编辑的特定字段不可编辑
        if (CollectionUtils.isNotEmpty(currentVersionCampaignDetail.getActivityInfos())) {
            currentVersionCampaignDetail.getActivityInfos().forEach(lv2 -> {
                if (ActivityStatusEnum.ACTIVATED.getStatusCode().equals(lv2.getStatus()) || lv2.getIsExistPassVersion()) {
                    // 变更首次进入OR变更保存后再编辑
                    lv2.setIsCanEditLv2Subject(false);
                }

                //  设置lv2上一个版本的LV金额、币种、最新汇率
                setLv2BeforeVersionInfo(lv2,  oldVersionDetail);

                List<ActivityHistoryInfoVo> activityL3s = lv2.getActivityL3s();
                if (CollectionUtils.isNotEmpty(activityL3s)) {
                    activityL3s.forEach(lv3 -> {
                        // 针对具体的lv3下是否有PO
                        if (Strings.isNotBlank(lv3.getActivityId())) {
                            List<String> lv3ActivityIds = new ArrayList<>();
                            lv3ActivityIds.add(lv3.getActivityId());
                            List<String> poIds = campaignDao.getPobyActivityId(lv3ActivityIds);
                            if (CollectionUtils.isNotEmpty(poIds)) {
                                // lv3下面存在有效PO, 部分字段不让编辑
                                lv3.setIsCanEditApplyAmount(false);
                                lv3.setIsCanEditPurchaseSubject(false);
                            }
                            // 对于已启动的status=2，设置为不可编辑状态
                            if (ActivityStatusEnum.ACTIVATED.getStatusCode().equals(lv3.getStatus()) || lv3.getIsExistPassVersion()) {
                                // 变更首次进入OR变更保存后再编辑
                                lv3.setIsCanEditLv3Subject(false);
                                lv3.setIsCanEditCurrency(false);
                            }

                            //  设置lv3上一个版本的LV金额、币种、最新汇率
                            setLv3BeforeVersionInfo(lv3,  oldVersionDetail);
                        }
                    });
                }
            });
        }
    }

    /**
     * 组装lv2,lv3数据是否显示删除按钮: 仅出现过一次且未审批通过的就可以删除，  其余都不能删除
     *
     * @param currentVersionCampaign 当前campaign版本
     */
    private void buildLv2Lv3IsShowDeleteBtn(CampaignHistoryInfoVo currentVersionCampaign, String detailScene, List<ExchangeRateInfoVo> rates) {
        if (CollectionUtils.isEmpty(currentVersionCampaign.getActivityInfos())) {
            return;
        }
        List<ActivityHistoryInfoVo> currentLv2s = currentVersionCampaign.getActivityInfos();
        List<String> currentLvIds =
                currentLv2s.stream().map(ActivityHistoryInfoVo::getActivityId).collect(Collectors.toList());
        currentLv2s.stream().map(lv2 -> lv2.getActivityL3s().stream().map(ActivityHistoryInfoVo::getActivityId).collect(Collectors.toList())).forEach(currentLvIds::addAll);

        Wrapper<ActivityHistoryInfoVo> wrapper = new EntityWrapper<>();
        wrapper.in("activity_id", currentLvIds);
        wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> activityHistoryInfoVos = activityHisInfoService.selectList(wrapper);

        // 判断本次的LV2,LV3是否显示删除按钮
        for (ActivityHistoryInfoVo lv2 : currentLv2s) {
            // 判断该lv2之前是否审批过了
            List<ActivityHistoryInfoVo> filterLv2ActivityInfos =
                    activityHistoryInfoVos.stream().filter(item -> StringUtils.equals(item.getActivityId(),
                            lv2.getActivityId())).collect(Collectors.toList());

            boolean isExistPassVersionL2 = filterLv2ActivityInfos.size() > 1 || (filterLv2ActivityInfos.size() == 1 && !ActivityStatusEnum.DRAFT.getStatusCode().equals(lv2.getStatus()));
            lv2.setIsExistPassVersion(isExistPassVersionL2); // l2是否存在通过的版本
            lv2.setIsShowDeleteBtn(isNewAddLv(currentVersionCampaign, filterLv2ActivityInfos.size()));
            if (!CommonConstant.DETAIL_SCENE.equalsIgnoreCase(detailScene)) {
                if (!isExistPassVersionL2) {
                    // 不存在历史版本， 就取最新的， 存在就不处理
                    BigDecimal latestRateL2Val = getLatestRate(lv2.getCurrency(),rates);
                    lv2.setInitRate(latestRateL2Val);
                    lv2.setLatestRate(latestRateL2Val);
                }
            }
            // lv2编辑按钮控制:除了拆分驳回且lv之前审批过了不显示，  其余场景都显示按钮
            lv2.setIsShowEditBtn(isShowEditBtnCtrLv2(currentVersionCampaign, filterLv2ActivityInfos.size(), detailScene));

            if (CollectionUtils.isNotEmpty(lv2.getActivityL3s())) {
                // 判断该lv3之前是否审批过了
                lv2.getActivityL3s().forEach(lv3 -> {
                    List<ActivityHistoryInfoVo> filterLv3ActivityInfos =
                            activityHistoryInfoVos.stream().filter(item -> StringUtils.equals(item.getActivityId(),
                                    lv3.getActivityId())).collect(Collectors.toList());
                    boolean isExistPassVersionL3 = filterLv3ActivityInfos.size() > 1 || !ActivityStatusEnum.DRAFT.getStatusCode().equals(lv3.getStatus());
                    lv3.setIsExistPassVersion(isExistPassVersionL3); // l3是否存在通过的版本
                    lv3.setIsShowDeleteBtn(isNewAddLv(currentVersionCampaign, filterLv3ActivityInfos.size()));
                    if (!CommonConstant.DETAIL_SCENE.equalsIgnoreCase(detailScene)) {
                        if (!isExistPassVersionL3) {
                            BigDecimal latestRateL3Val = getLatestRate(lv3.getFinanceInfo().getCurrency(),rates);
                            lv3.setInitRate(latestRateL3Val);
                            lv3.setLatestRate(latestRateL3Val);
                        }
                    }

                    // lv2编辑按钮控制:拆分驳回且lv之前审批过了不显示 + 变更自行采购不显示  其余场景都显示按钮
                    lv3.setIsShowEditBtn(isShowEditBtnCtrLv3(currentVersionCampaign, lv3,
                            filterLv3ActivityInfos.size(), detailScene));
                });
            }
        }
    }

    public BigDecimal getLatestRate(String currency, List<ExchangeRateInfoVo> rates) {
        if (CollectionUtils.isEmpty(rates)) {
            return null;
        }
        List<ExchangeRateInfoVo> filterRates = rates.stream().filter(item -> StringUtils.equals(currency, item.getFromCurrency())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filterRates)) {
            return null;
        }
        // 最新汇率
        return filterRates.get(0).getRate();
    }

    /**
     * 是否本次新增的lv 显示：只出现过1次 + 非审批通过,即本次新增的
     *
     * @param currentVersionCampaign
     * @param count
     * @return
     */
    private boolean isNewAddLv(CampaignHistoryInfoVo currentVersionCampaign, int count) {
        boolean isFirst = count == 1;

        boolean isApprovalPass = StringUtils.equals(currentVersionCampaign.getStatusCode(),
                CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode());

        return isFirst && !isApprovalPass;
    }

    /**
     * lv2是否能编辑控制；不显示： 拆分入口 + 非新增
     *
     * @param currentVersionCampaign
     * @param count
     * @return
     */
    private boolean isShowEditBtnCtrLv2(CampaignHistoryInfoVo currentVersionCampaign, int count, String detailScene) {
        // 编辑按钮控制: 拆分入口 + 非新增不显示，  其余场景都显示编辑按钮
        boolean isSplit = CommonConstant.DETAIL_SCENE.equalsIgnoreCase(detailScene);

        boolean isNewAdd = isNewAddLv(currentVersionCampaign, count);

        return !isSplit || isNewAdd;
    }

    /**
     * lv2是否能编辑控制；不显示： 变更入口+ 非新增 + 自行采购  或者 拆分入口+ 非新增， 不让编辑
     *
     * @param currentVersionCampaign
     * @param lv3Vo
     * @param count
     * @return
     */
    private boolean isShowEditBtnCtrLv3(CampaignHistoryInfoVo currentVersionCampaign, ActivityHistoryInfoVo lv3Vo,
                                        int count, String detailScene) {
        // 是否新增
        boolean isNewAdd = isNewAddLv(currentVersionCampaign, count);

        // 是否拆分入口
        boolean isSplit = CommonConstant.DETAIL_SCENE.equalsIgnoreCase(detailScene);

        // 是否自行采购
        boolean isSelfPurchase = CommonConstant.PURCHASE_TYPE_SELF_CN.equals(lv3Vo.getPurchaseInfo().getPurchaseType())
                || CommonConstant.PURCHASE_TYPE_SELF_EN.equals(lv3Vo.getPurchaseInfo().getPurchaseType());

        // 不管入口， 非新增 + 自行采购不让编辑
        if (!isNewAdd && isSelfPurchase) {
            return false;
        }

        // 拆分入口+ 非新增， 不让编辑
        if (isSplit && !isNewAdd) {
            return false;
        }

        return true;
    }

    /**
     * 获取上一版本的campaign数据
     *
     * @param campaignId
     * @param campaignVersion
     */
    public CampaignHistoryInfoVo getPreviousVersionCampaign(String campaignId, String campaignVersion) {
        Wrapper<CampaignHistoryInfoVo> camWrapper1 = new EntityWrapper<>();
        camWrapper1.eq("campaign_id", campaignId);
        camWrapper1.lt("version", campaignVersion);
        camWrapper1.orderBy("id", false);
        camWrapper1.last("limit 1");
        CampaignHistoryInfoVo previousVersionCampaign = selectOne(camWrapper1);

        return previousVersionCampaign;
    }

    public CampaignHistoryInfoVo getCampaignHistoryDetail(String campaignId, Integer id) {
        String lang = RequestUtil.getLang();
        // 1.查询Campaign信息
        CampaignHistoryInfoVo campaignHistoryInfoVo = selectById(id);
        List<AreaEntity> areaEntities = areaService.selectList(null);
        if (CommonConstant.EN_LANGUAGE.equals(lang)) {
            for (AreaEntity entity : areaEntities) {
                entity.setNameCn(entity.getNameEn());
            }
        }
        List<BudgetTypeEntity> budgetTypeEntities = budgetTypeService.selectList(null);
        if (CommonConstant.EN_LANGUAGE.equals(lang)) {
            for (BudgetTypeEntity type : budgetTypeEntities) {
                type.setNameCn(type.getNameEn());
            }
        }

        Map<String, BudgetTypeEntity> collect1 = budgetTypeEntities.stream().collect(Collectors.toMap(BudgetTypeEntity::getCode, budgetTypeEntity -> budgetTypeEntity, (a, b) -> b));
        Map<String, AreaEntity> collect = areaEntities.stream().collect(Collectors.toMap(AreaEntity::getCode, areaEntity -> areaEntity, (a, b) -> b));
        campaignHistoryInfoVo.setBudgetBelongAreaName(collect.get(campaignHistoryInfoVo.getBudgetBelongAreaCode()).getNameCn());
        if (StringUtils.isNotBlank(campaignHistoryInfoVo.getBudgetBelongCountryCode())) {
            campaignHistoryInfoVo.setBudgetBelongCountryName(collect.get(campaignHistoryInfoVo.getBudgetBelongCountryCode()).getNameCn());
        }
        campaignHistoryInfoVo.setRepresentativeName(collect.get(campaignHistoryInfoVo.getRepresentative()).getNameCn());
        campaignHistoryInfoVo.setSubmitterRepresentativeName(collect.get(campaignHistoryInfoVo.getSubmitterRepresentative()).getNameCn());
        campaignHistoryInfoVo.setBudgetTypeName(collect1.get(campaignHistoryInfoVo.getBudgetType()).getNameCn());
        if (null == campaignHistoryInfoVo) {
            throw new RRException("queryCampaignHisInfo is null");
        }

        // 2.封装附件信息
        Wrapper<AttachmentInfo> attachWrapper = new EntityWrapper<>();
        attachWrapper.eq("ref_id", id);
        attachWrapper.eq("status", MyConstant.DEL_STAUTS_NORMAL);
        List<AttachmentInfo> attachmentInfoVos = attachmentInfoService.selectList(attachWrapper);

        campaignHistoryInfoVo.setAttachmentInfos(attachmentInfoVos);
        campaignHistoryInfoVo.setAttachmentsName(attachmentInfoVos.stream().map(AttachmentInfo::getAttachmentName).collect(Collectors.joining(",")));

        // 3.组装活动信息
        List<ActivityHistoryInfoVo> activityInfos = activityHisInfoService.getActivityHisByCampaignId(campaignId, id);
        campaignHistoryInfoVo.setActivityInfos(activityInfos);

        // 4.组装当前节点剩余处理人, campaign状态
        if (CampaignStatusEnum.DRAFT.getStatusCode().equals(campaignHistoryInfoVo.getStatus())) {
            List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
            Map<Long, String> userMap = sysUserEntities.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUserCard));
            campaignHistoryInfoVo.setCurrentOperator(userMap.get(Long.valueOf(campaignHistoryInfoVo.getCreatedBy())));
        } else {
            campaignHistoryInfoVo.setCurrentOperator(getNodeOperator(campaignHistoryInfoVo.getProcessId()));
        }

        campaignHistoryInfoVo.setStatusCode(campaignHistoryInfoVo.getStatus());
        String status = CommonConstant.EN_LANGUAGE.equals(lang) ? CampaignStatusEnum.getStatusNameEnByCode(campaignHistoryInfoVo.getStatus()) : CampaignStatusEnum.getStatusNameCnByCode(campaignHistoryInfoVo.getStatus());
        campaignHistoryInfoVo.setStatus(status);

        // 设置campaign金额
        setCamapignAmount(campaignHistoryInfoVo);

        // 设置L2关闭按钮是否显示
        setL2CloseBtnIsShow(campaignHistoryInfoVo);

        return campaignHistoryInfoVo;
    }

    private void setL2CloseBtnIsShow(CampaignHistoryInfoVo campaignHistoryInfoVo) {
        Long currentUserId = ShiroUtils.getUserId();
        // 校验是否是当前campaign的campaignOwner或者PA或者创建人角色
        boolean isRoleOk = (StringUtils.equals(campaignHistoryInfoVo.getCampaignPa(), currentUserId.toString())
                || StringUtils.equals(campaignHistoryInfoVo.getCampaignOwner(), currentUserId.toString())
                || StringUtils.equals(campaignHistoryInfoVo.getCreatedBy(), currentUserId.toString()));

        if (!isRoleOk) {
            return;
        }
        List<ActivityHistoryInfoVo> l2Vos = campaignHistoryInfoVo.getActivityInfos();
        if (CollectionUtils.isEmpty(l2Vos)) {
            return;
        }

        for (ActivityHistoryInfoVo l2 : l2Vos) {
            if (ActivityStatusEnum.ACTIVATED.getStatusCode().equals(l2.getStatus())) {
                List<ActivityHistoryInfoVo> l3Vos = l2.getActivityL3s();
                if (CollectionUtils.isNotEmpty(l3Vos)) {
                    List<String> l3Status = l3Vos.stream().map(ActivityHistoryInfoVo::getStatus).distinct().collect(Collectors.toList());
                    if (l3Status.size() == 1 && ActivityStatusEnum.CLOSED.getStatusCode().equals(l3Status.get(0))) {
                        l2.setIsShowCloseBtn(true);
                    }
                }
            }
        }
    }

    private String getNodeOperator(Integer processId) {
        if (null == processId) {
            return null;
        }
        List<ProcessTaskInfoVo> taskInfos = taskInfoDao.selectListByProcessId(processId);

        List<String> operators = taskInfos.stream().filter(item -> ProcessTaskStatusEnum.CURRENT.getStatusCode().equals(item.getTaskStatus())).map(taskInfoVo -> sysUserService.selectById(taskInfoVo.getAssign())).map(SysUserEntity::getUserCard).distinct().collect(Collectors.toList());
        return String.join(",", operators);
    }

    @Override
    public List<CampaignHistoryInfoVo> findList(Map<String, Object> params) {
        List<CampaignHistoryInfoVo> list = baseMapper.findList(params);
        if (MY.equals(params.get("type"))) {
            filterMyAcceptance(list);
        }
        return list;
    }


    /**
     * 查询Campaign分页列表
     *
     * @return
     */
    @Override
    public PageUtils list(Map<String, Object> params, Long userId) {

        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        int page = Integer.valueOf(params.get("currentPage").toString());
        int size = Integer.valueOf(params.get("pagesize").toString());
        params.put("page", (page - 1) * size);
        params.put("size", size);
        params.put("status", CampaignStatusEnum.buildCampaignStatusList(params.get("status")));
        // 1.查询Campaign信息
        List<CampaignHistoryInfoVo> campaigns = baseMapper.findList(params);
        if (MY.equals(params.get("type"))) {
            filterMyAcceptance(campaigns);
        }
        // 区域过滤, 我的campaign不需要
        if (!MY.equals(params.get("type"))) {
            campaigns = getBelongExecuteDatas(campaigns, userId);
        }


        // 2.查询地区国家、预算分类列表
        List<AreaEntity> areaEntities = areaService.selectList(null);
        List<BudgetTypeEntity> budgetTypeEntities = budgetTypeService.selectList(null);
        List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
        // 3.收集成map方便循环里去匹配
        Map<String, String> areaMap = areaEntities.stream().collect(Collectors.toMap(AreaEntity::getCode, areaEntity -> isChinese ? areaEntity.getNameCn() : areaEntity.getNameEn(), (a, b) -> b));
        Map<String, String> budgetTypeMap = budgetTypeEntities.stream().collect(Collectors.toMap(BudgetTypeEntity::getCode, budgetTypeEntity -> isChinese ? budgetTypeEntity.getNameCn() : budgetTypeEntity.getNameEn(), (a, b) -> b));
        Map<Long, String> userMap = sysUserEntities.stream().collect(Collectors.toMap(SysUserEntity::getUserId, sysUserEntity -> sysUserEntity.getUserCard().trim(), (a, b) -> b));

        // 判断当前用户是否是CmapaignOwner 或 CampaignPA角色
        boolean checkRoleName = campaignCommonService.checkSysUserRole(userId, Arrays.asList(RoleNameEnum.CAMPAIGN_PA.getRoleNameEn(), RoleNameEnum.CAMPAIGN_OWNER.getRoleNameEn()));
        int totalSize = campaigns.size();
        // 先分页, 再处理数据
        campaigns = (List<CampaignHistoryInfoVo>) PageUtil.limit(campaigns, size, page);

        // 返回数据处理
        List<CampaignListVo> res = new ArrayList<>();
        for (CampaignHistoryInfoVo campaign : campaigns) {
            CampaignListVo campaignVo = new CampaignListVo();
            BeanUtils.copyProperties(campaign, campaignVo);
            // 4.获取附件信息
            List<AttachmentInfo> attachmentInfos = attachmentInfoService.queryByRefId(campaign.getId());
            campaignVo.setAttachmentInfos(attachmentInfos);
            // 5.获取预算分类信息
            campaignVo.setBudgetType(budgetTypeMap.get(campaign.getBudgetType()));
            // 6.获取地区/国家信息
            campaignVo.setBudgetBelongArea(areaMap.get(campaign.getBudgetBelongAreaCode()));
            campaignVo.setBudgetBelongCountry(areaMap.get(campaign.getBudgetBelongCountryCode()));
            campaignVo.setRepresentative(areaMap.get(campaign.getRepresentative()));

            String campaignOwner = (StringUtils.isEmpty(campaign.getCampaignOwner()) ? null : userMap.get(Long.valueOf(campaign.getCampaignOwner())));
            campaignVo.setCampaignOwner(campaignOwner);

            String campaignPa = (StringUtils.isEmpty(campaign.getCampaignPa()) ? null : userMap.get(Long.valueOf(campaign.getCampaignPa())));
            campaignVo.setCampaignPa(campaignPa);

            String createdBy = (StringUtils.isEmpty(campaign.getCreatedBy()) ? null : userMap.get(Long.valueOf(campaign.getCreatedBy())));
            campaignVo.setCreatedBy(createdBy);

            String isValidVersionCode = campaignVo.getIsValidVersion();
            String isValidVersion = CommonConstant.VALID_VERISON_EN.equals(isValidVersionCode) ?
                    (isChinese ? CommonConstant.VALID_VERISON_CN : CommonConstant.VALID_VERISON_EN) :
                    (isChinese ? CommonConstant.INVALID_VERISON_CN : CommonConstant.INVALID_VERISON_EN);
            campaignVo.setIsValidVersion(isValidVersion);

            // 设置关闭按钮是否显示， campaign审批通过，LV2已全部关闭、owner/pa/创建人可见
            campaignVo.setIsShowCloseBtn(checkIsShowCloseBtn(campaign, checkRoleName, userId));

            // 设置取消按钮是否显示, 当前用户是提交人 + 状态 + PA或者Owner角色,
            campaignVo.setIsShowCancelBtn(checkIsShowCancelBtn(campaignVo.getStatus(), campaign.getApplyUser(), userId,
                    checkRoleName));

            // 是否显示变更按钮, 最大有效版本的campaign状态是否是审批通过状态且当前也是审批通过的，并且有效版本,并且不能将关闭的， 是就显示， 否则不显示
            campaignVo.setIsShowChangeBtn(checkIsShowChangeBtn(campaignVo.getCampaignId(), campaignVo.getStatus(),
                    isValidVersionCode) && !campaignVo.getIsShowCloseBtn());
            res.add(campaignVo);
        }

        return new PageUtils(res, totalSize, size, page);
    }

    /**
     * 累计申请金额USD = 子L2申请金额USD汇总
     * 累计验收金额USD = 子L3验收通过的金额USD汇总
     * 回冲金额USD(关闭时才显示) = 累计申请 - 累计验收
     *
     * @param campaignHistoryInfoVo
     */
    private void setCamapignAmount(CampaignHistoryInfoVo campaignHistoryInfoVo) {
        List<ActivityHistoryInfoVo> l2Infos = campaignHistoryInfoVo.getActivityInfos();
        if (CollectionUtils.isEmpty(l2Infos)) {
            return;
        }
        // 累计申请金额USD
        BigDecimal applyAmountTotal = ZERO;
        List<ActivityHistoryInfoVo> l3Infos = new ArrayList<>();
        for (ActivityHistoryInfoVo lv2 : l2Infos) {
            if (null != lv2.getAmount() && null != lv2.getLatestRate()) {
                applyAmountTotal = applyAmountTotal.add(MathUtils.multiplyNull(lv2.getAmount(), lv2.getLatestRate()));
            }
            List<ActivityHistoryInfoVo> currentL3Infos = lv2.getActivityL3s();
            if (CollectionUtils.isNotEmpty(currentL3Infos)) {
                l3Infos.addAll(currentL3Infos);
            }
        }
        campaignHistoryInfoVo.setApplyAmountUsd(applyAmountTotal);

        if (CollectionUtils.isEmpty(l3Infos)) {
            return;
        }
        // 累计验收金额USD
        Wrapper<AcceptanceFormInfo> accWrapper = new EntityWrapper<>();
        accWrapper.in("activity_id", l3Infos.stream().map(ActivityHistoryInfoVo::getActivityId).collect(Collectors.toList()));
        accWrapper.eq("status", AcceptanceFormStatusEnum.APPROVAL_SUCCESS.getStatusCode());
        accWrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoService.selectList(accWrapper);
        if (CollectionUtils.isEmpty(acceptanceFormInfos)) {
            return;
        }
        BigDecimal acceptancedAmountTotal = acceptanceFormInfos.stream().map(item -> item.getAcceptanceAmount().multiply(item.getExchangeRate())).reduce(BigDecimal::add).orElse(null);
        campaignHistoryInfoVo.setAcceptancedAmountUsd(acceptancedAmountTotal);
    }



    /**
     * 设置取消按钮是否显示, campaign审批通过，LV2已全部关闭、owner/pa/创建人可见
     *
     * @param campaignHistoryInfoVo
     * @return
     */
    private boolean checkIsShowCloseBtn(CampaignHistoryInfoVo campaignHistoryInfoVo, boolean checkRoleName, Long userId) {
        if (!CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(campaignHistoryInfoVo.getStatus())) {
            return false;
        }
        if (!(checkRoleName || StringUtils.equals(campaignHistoryInfoVo.getCreatedBy(), userId.toString()))) {
            return false;
        }
        Wrapper<ActivityHistoryInfoVo> acWrapper = new EntityWrapper<>();
        acWrapper.eq("ref_id", campaignHistoryInfoVo.getId());
        acWrapper.eq("level", 2);
        acWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> activityHistoryInfoVos = activityHisInfoService.selectList(acWrapper);
        if (CollectionUtils.isEmpty(activityHistoryInfoVos)) {
            // 无L2
            return false;
        }
        for (ActivityHistoryInfoVo actVo: activityHistoryInfoVos) {
            if (!ActivityStatusEnum.CLOSED.getStatusCode().equals(actVo.getStatus())) {
                // 存在未关闭的L2
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否显示取消按钮:当前用户是创建人 + 状态 + PA或者Owner角色
     *
     * @return boolean
     */
    public boolean checkIsShowCancelBtn(String recordStatus, String recordCreatedByUserId, Long currentUserId,
                                        boolean checkRoleName) {
        if (StringUtils.isEmpty(recordCreatedByUserId)) {
            // 数据的创建人为空
            return false;
        }
        if (!recordCreatedByUserId.equals(currentUserId.toString())) {
            // 登录用户不是创建人
            return false;
        }

        if (!CampaignStatusEnum.canCancelCampaignStatusCodes().contains(recordStatus)) {
            return false;
        }
        return checkRoleName;
    }

    private boolean checkIsShowChangeBtn(String campaignId, String currentStatusCode, String isValidVersionCode) {
        if (!StringUtils.equals(CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode(), currentStatusCode)) {
            // 非审批通过的
            return false;
        }

        if (CommonConstant.INVALID_VERISON_EN.equals(isValidVersionCode)) {
            // 无效状态
            return false;
        }

        // 审批通过，继续判断最大有效版本的campaign状态是否是审批通过状态且当前也是审批通过的， 是就显示， 否则不显示
        CampaignHistoryInfoVo campaignLatestValidVersion = getCampaignLatestValidVersion(campaignId);

        return (null != campaignLatestValidVersion && CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(campaignLatestValidVersion.getStatus())
                && CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(currentStatusCode));
    }

    private void filterMyAcceptance(List<CampaignHistoryInfoVo> campaigns) {
        // 我的相关过滤
        Map<String, Object> userMap = new HashMap<>();
        SysUserEntity userEntity = ShiroUtils.getUserEntity();
        userMap.put("userId", userEntity.getUserId());
        userMap.put("userName", userEntity.getUsername());
        List<Integer> myCampaignId = campaignDao.getMyCampaignId(userMap);
        Iterator<CampaignHistoryInfoVo> iterator = campaigns.iterator();
        while (iterator.hasNext()) {
            if (!myCampaignId.contains(iterator.next().getId())) {
                iterator.remove();
            }
        }
    }

    /**
     * 过滤得到用户归属区域的数据
     *
     * @param campaigns 未过滤前的Campaign执行明细数据
     * @param userId    用户id
     * @return 过滤后得到的数据
     */
    private List<CampaignHistoryInfoVo> getBelongExecuteDatas(List<CampaignHistoryInfoVo> campaigns, Long userId) {
        Map<String, Set<String>> areaMap = getBelogAreaCodes(userId);
        Set<String> lv1Set = areaMap.get("lv1AreaCodes");
        Set<String> lv2Set = areaMap.get("lv2AreaCodes");
        Set<String> lv3Set = areaMap.get("lv3AreaCodes");
        if (CollectionUtils.isNotEmpty(lv1Set)) {
            return campaigns;
        }

        if (CollectionUtils.isNotEmpty(lv2Set)) {
            lv2Set.addAll(lv3Set);
            return campaigns.stream().filter(vo -> lv2Set.contains(vo.getBudgetBelongAreaCode())).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(lv3Set)) {
            return campaigns.stream().filter(vo -> lv3Set.contains(vo.getRepresentative())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Map<String, Set<String>> getBelogAreaCodes(Long userId) {
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.eq("user_id", userId);
        List<SysUserArea> sysList = sysUserAreaDao.selectList(wrapper);
        Set<String> lv1Set = new HashSet<>();
        Set<String> lv2Set = new HashSet<>();
        Set<String> lv3Set = new HashSet<>();
        for (SysUserArea sysUserArea : sysList) {
            if (AreaLevelEnum.WORLDWIDE.getLevel().equals(sysUserArea.getLevel())) {
                lv1Set.add(sysUserArea.getAreaCode());
                break;
            }
            if (AreaLevelEnum.REGION.getLevel().equals(sysUserArea.getLevel())) {
                lv2Set.add(sysUserArea.getAreaCode());
                continue;
            }
            if (AreaLevelEnum.REPRESENT_OFFICE.getLevel().equals(sysUserArea.getLevel())) {
                lv3Set.add(sysUserArea.getAreaCode());
            }
        }

        Map<String, Set<String>> areaMap = new HashMap<>();
        areaMap.put("lv1AreaCodes", lv1Set);
        areaMap.put("lv2AreaCodes", lv2Set);
        areaMap.put("lv3AreaCodes", lv3Set);
        return areaMap;
    }

    /**
     * campaign字段差异化比较
     *
     * @param newVersionDetail CampaignHistoryInfoVo
     */
    public void compareDifferent(CampaignHistoryInfoVo oldVersionDetail, CampaignHistoryInfoVo newVersionDetail) {
        if (null == oldVersionDetail) {
            // 没有上一版本
            return;
        }

        // 比较campaign(地区部、代表处、国家、campaign名称、campaign描述、campaignOwner、campaignPA)
        newVersionDetail.setChangedFields(CommonUtil.comparObjWithAnnotion(oldVersionDetail, newVersionDetail));

        List<ActivityHistoryInfoVo> newlv2s = newVersionDetail.getActivityInfos();
        newlv2s.forEach(newLv2Obj -> {
            // 1.1 比较lv2
            List<ActivityHistoryInfoVo> oldLv2s = oldVersionDetail.getActivityInfos().stream().filter(item -> StringUtils.equals(item.getActivityId(), newLv2Obj.getActivityId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(oldLv2s)) {
                // 此次新增lv2
                newLv2Obj.setChangedFields(CommonUtil.getFieldsWithAnnotion(newLv2Obj, DifferentFields.class));
                // 此次新增lv3
                List<ActivityHistoryInfoVo> newlv3s = newLv2Obj.getActivityL3s();
                if (CollectionUtils.isNotEmpty(newlv3s)) {
                    newlv3s.forEach(newLv3Obj -> {
                        List<String> lv3Diffs = CommonUtil.getFieldsWithAnnotion(newLv3Obj, DifferentFields.class);
                        // 采购差异
                        List<String> purchaseDiffs = CommonUtil.getFieldsWithAnnotion(newLv3Obj.getPurchaseInfo(), DifferentFields.class).stream().map(str -> "purchaseInfo." + str).collect(Collectors.toList());
                        lv3Diffs.addAll(purchaseDiffs);
                        // 财经差异
                        List<String> financeDiffs = CommonUtil.getFieldsWithAnnotion(newLv3Obj.getFinanceInfo(), DifferentFields.class).stream().map(str -> "financeInfo." + str).collect(Collectors.toList());
                        lv3Diffs.addAll(financeDiffs);
                        newLv3Obj.setChangedFields(lv3Diffs);
                    });
                }
                return;
            }
            newLv2Obj.setChangedFields(CommonUtil.comparObjWithAnnotion(oldLv2s.get(0), newLv2Obj));

            // 1.2 比较lv3
            List<ActivityHistoryInfoVo> newlv3s = newLv2Obj.getActivityL3s();
            newlv3s.forEach(newLv3Obj -> {
                // 找到对应的老lv3对象
                oldLv2s.forEach(oldLv2Obj -> {
                    List<String> lv3Diffs = new ArrayList<>();
                    List<ActivityHistoryInfoVo> oldLv3s = oldLv2Obj.getActivityL3s().stream().filter(item -> StringUtils.equals(item.getActivityId(), newLv3Obj.getActivityId())).collect(Collectors.toList());
                    ActivityHistoryInfoVo oldLv3Obj = CollectionUtils.isEmpty(oldLv3s) ? null : oldLv3s.get(0);
                    PurchaseInfo purchaseInfo = CollectionUtils.isEmpty(oldLv3s) ? null : oldLv3s.get(0).getPurchaseInfo();
                    FinanceInfo financeInfo = CollectionUtils.isEmpty(oldLv3s) ? null : oldLv3s.get(0).getFinanceInfo();
                    lv3Diffs.addAll(CommonUtil.comparObjWithAnnotion(oldLv3Obj, newLv3Obj));
                    // 采购科目比较
                    List<String> purchaseDiffs = CommonUtil.comparObjWithAnnotion(purchaseInfo, newLv3Obj.getPurchaseInfo()).stream().map(str -> "purchaseInfo." + str).collect(Collectors.toList());
                    lv3Diffs.addAll(purchaseDiffs);
                    // 财经比较
                    List<String> financeDiffs = CommonUtil.comparObjWithAnnotion(financeInfo, newLv3Obj.getFinanceInfo()).stream().map(str -> "financeInfo." + str).collect(Collectors.toList());
                    lv3Diffs.addAll(financeDiffs);
                    newLv3Obj.setChangedFields(lv3Diffs);
                });
            });
        });
    }

    public void updateApprover(CampaignHistoryInfoVo info) {
        campaignHistoryInfoDao.updateApprover(info);
    }

    /**
     * 获取上一有效版本的campaign信息
     *
     * @param campaignId 当前campaignId
     * @param id         当前campaign的自增主键
     * @return
     */
    public CampaignHistoryInfoVo getCampaignPreviousValidVersion(String campaignId, Integer id) {
        // 不用加状态查询
        Wrapper<CampaignHistoryInfoVo> acWrapper = new EntityWrapper<>();
        acWrapper.eq("campaign_id", campaignId);
        acWrapper.lt("id", id);
        acWrapper.eq("is_valid_version", CommonConstant.VALID_VERISON_EN);
        acWrapper.orderBy("id", false);
        acWrapper.last("limit 1");

        CampaignHistoryInfoVo previousVersionCampaignInfo = selectOne(acWrapper);
        return previousVersionCampaignInfo;
    }

    /**
     * 获取campaign最新的有效版本信息
     *
     * @param campaignId
     * @return
     */
    public CampaignHistoryInfoVo getCampaignLatestValidVersion(String campaignId) {
        Wrapper<CampaignHistoryInfoVo> wrapper = new EntityWrapper<>();
        wrapper.eq("campaign_id", campaignId);
        wrapper.eq("is_valid_version", CommonConstant.VALID_VERISON_EN);
        wrapper.orderBy("id", false);
        wrapper.last("limit 1");

        CampaignHistoryInfoVo latestVersionCampaignInfo = selectOne(wrapper);
        return latestVersionCampaignInfo;
    }

    /**
     * Campaign执行信息查询
     *
     * @return
     */
    @Override
    public PageUtils queryCampaignExecuteInformation(Map<String, Object> params, Long userId) {
        int page = Integer.parseInt(params.get("currentPage").toString());
        int size = Integer.parseInt(params.get("pageSize").toString());
        String campaignId = (String) params.get("campaignId");
        Integer id = Integer.valueOf(params.get("id").toString());

        // 查询campaign执行信息
        List<CampaignExecuteInformationVo> infos = queryCampaignExecuteInformationHandle(campaignId, id, userId);
        if (CollectionUtils.isEmpty(infos)) {
            return new PageUtils(new ArrayList<>(), 0, size, page);
        }

        return new PageUtils(PageUtil.limit(infos, size, page), infos.size(), size, page);
    }

    /**
     * Campaign执行信息导出
     *
     * @return
     */
    @Override
    public void exportCampaignExecuteInformation(Map<String, Object> params, Long userId, HttpServletResponse response) throws IOException {
        String campaignId = (String) params.get("campaignId");
        Integer id = Integer.valueOf(params.get("id").toString());
        boolean isChinese = CommonConstant.ZH_CN_LANGUAGE.equals(params.get("language"));

        // 查询campaign执行信息
        long time1 = System.currentTimeMillis();
        List<CampaignExecuteInformationExportCnVo> exportVos = exportCampaignExecuteInformationHandle(campaignId, id, isChinese);
        long time2 = System.currentTimeMillis();
        log.info("exportCampaignExecuteInformation query end =====>{}", time2 - time1);

        String title = isChinese?"Campaign执行清单" : "Campaign execution checklist";
        String fileName = title + Dates.getDateString();

        // 要合并的单元格数据
        Map<String, List<RowRangeDto>> strategyMap = addMerStrategy(exportVos);
        long time3 = System.currentTimeMillis();
        log.info("exportCampaignExecuteInformation addMerStrategy end =====>{}", time3 - time2);
        if(CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))){
            CommonsUtils.exportExcel(title, fileName, exportVos, CampaignExecuteInformationExportEnVo.class, response, strategyMap, 2);
        }
        else{
            CommonsUtils.exportExcel(title, fileName, exportVos, CampaignExecuteInformationExportCnVo.class, response, strategyMap, 2);
        }
        //调用接口导出表格
        long time4 = System.currentTimeMillis();
        log.info("exportCampaignExecuteInformation end =====>{}", time4 - time3);
    }

    private List<CampaignExecuteInformationExportCnVo> exportCampaignExecuteInformationHandle(String campaignId, Integer id, boolean isChinese) {
        log.info("queryCampaignExecuteInformationHandle campaignId:{}, id:{}", campaignId, id);
        // 1. 查询L2数据
        List<CampaignExecuteInformationVo> lv2Infos = baseMapper.queryCampaignExecuteInformation4Lv2(id);
        if (CollectionUtils.isEmpty(lv2Infos)) {
            return new ArrayList<>();
        }

        // 1.2业务科目类型映射
        List<ActivityType> activityTypeVoList = activityTypeDao.selectList(null);
        Map<String, String> activityTypeMap = new HashMap<>();
        if (isChinese) {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameCn()));
        } else {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameEn()));
        }
        List<SysDictEntity> purchaseTypeDict = sysDictService.findByType("purchaseType");

        List<CampaignExecuteInformationExportCnVo> exportVos = new ArrayList<>();
        lv2Infos.forEach(lv2 -> {
            lv2.setLv2Status(isChinese ? ActivityStatusEnum.getStatusNameCnByCode(lv2.getLv2Status()) : ActivityStatusEnum.getStatusNameEnByCode(lv2.getLv2Status()));
            // 2.1 查询其子L3基础数据
            List<CampaignExecuteInformationLv3Vo> lv3Infos = baseMapper.queryCampaignExecuteInformation4Lv3(id, lv2.getLv2ActivityId());
            if (CollectionUtils.isEmpty(lv3Infos)) {
                // 无L3数据
                lv2.setLv2RemainingAmountIncludeTax(lv2.getLv2ApplyAmountIncludeTax());
                CampaignExecuteInformationExportCnVo exportVo = new CampaignExecuteInformationExportCnVo();
                BeanUtils.copyProperties(lv2, exportVo);
                exportVos.add(exportVo);
                return;
            }

            lv3Infos.forEach(lv3 -> {
                lv3.setActivityExecutorUserCard(campaignCommonService.getUserByUserId(lv3.getActivityExecutorUserId()).getUserCard());
                lv3.setLv3Subject(activityTypeMap.get(lv3.getLv3Subject()));
                lv3.setLv3Status(isChinese ? ActivityStatusEnum.getStatusNameCnByCode(lv3.getLv3Status()) : ActivityStatusEnum.getStatusNameEnByCode(lv3.getLv3Status()));
                lv3.setPrStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(lv3.getPrStatusCode()) : PrPoStatusEnum.getNameEnByCode(lv3.getPrStatusCode()));
                lv3.setPoStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(lv3.getPoStatusCode()) : PrPoStatusEnum.getNameEnByCode(lv3.getPoStatusCode()));
                if(!isChinese) {
                    purchaseTypeDict.forEach(item -> {
                        if (StringUtils.equals(item.getValue(), lv3.getPurchaseType())) {
                            lv3.setPurchaseType(item.getValueEn());
                        }
                    });
                }
                // 2.2 查询PO相关金额并设置金额值
                lv3.setLv3AcceptancedAmountUsdIncludeTax(MathUtils.round(baseMapper.getL3AcceptancedAmountTotal(lv3.getPrId()), 2)); // l3已验收金额
                if (StringUtils.isNotEmpty(lv3.getPoId())) {
                    lv3.setPoAmount(baseMapper.getPoAmountTotal(lv3.getPoId())); // PO总金额
                    lv3.setPoAcceptanceAmount(baseMapper.getPoAcceptancedAmountTotal(lv3.getPrId(), lv3.getPoId())); // PO已验收金额
                    lv3.setPoUnAcceptancedAmount(MathUtils.subtract(lv3.getPoAmount(), lv3.getPoAcceptanceAmount())); // PO待验收金额
                    lv3.setPoBillingedAmount(baseMapper.getPoBillingedAmountTotal(lv3.getPoId())); // 累计开票金额
                    lv3.setPoPaymentedAmount(baseMapper.getPoPaymentedAmountTotal(lv3.getPoId())); // 累计付款金额
                }
            });
            // 查询L2剩余可申请余额USD = L2申请金额 - L3申请金额汇总 + L3回冲金额汇总  - L2已回冲金额
            Map<String, BigDecimal> amountMap = getAmountMap(lv3Infos);
            BigDecimal l3ApplyAmountUsdTotal = amountMap.get("l3ApplyAmountUsdTotal");
            BigDecimal l3RecoveryAmountUsdTotal = amountMap.get("l3RecoveryAmountUsdTotal");
            BigDecimal lv2RemainingAmountIncludeTax = MathUtils.subtract(MathUtils.add(MathUtils.subtract(lv2.getLv2ApplyAmountUsdIncludeTax(), l3ApplyAmountUsdTotal),l3RecoveryAmountUsdTotal), lv2.getLv2RecoveryAmounUsdIncludeTax());
            lv2.setLv2RemainingAmountIncludeTax(lv2RemainingAmountIncludeTax);

            // 查询L2已验收金额USD = L3验收金额USD汇总
            lv2.setLv2ActuallyUsedAmountUsdIncludeTax(amountMap.get("lv3AcceptancedAmountUsdIncludeTaxTotal"));

            // 构建导出VO
            lv3Infos.forEach(lv3 -> {
                CampaignExecuteInformationExportCnVo exportVo = new CampaignExecuteInformationExportCnVo();
                BeanUtils.copyProperties(lv2, exportVo);
                BeanUtils.copyProperties(lv3, exportVo);
                exportVos.add(exportVo);
            });
        });

        return exportVos;
    }

    private List<CampaignExecuteInformationVo> queryCampaignExecuteInformationHandle(String campaignId, Integer id, Long userId) {
        log.info("queryCampaignExecuteInformationHandle campaignId:{}, id:{}", campaignId, id);
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        // 1. 查询L2数据
        List<CampaignExecuteInformationVo> lv2Infos = baseMapper.queryCampaignExecuteInformation4Lv2(id);
        if (CollectionUtils.isEmpty(lv2Infos)) {
            return new ArrayList<>();
        }

        // 1.2业务科目类型映射
        List<ActivityType> activityTypeVoList = activityTypeDao.selectList(null);
        Map<String, String> activityTypeMap = new HashMap<>();
        if (isChinese) {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameCn()));
        } else {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameEn()));
        }
        List<SysDictEntity> purchaseTypeDict = sysDictService.findByType("purchaseType");

        // 2. 查询其子L3数据
        lv2Infos.forEach(lv2 -> {
            lv2.setLv2Status(isChinese ? ActivityStatusEnum.getStatusNameCnByCode(lv2.getLv2Status()) : ActivityStatusEnum.getStatusNameEnByCode(lv2.getLv2Status()));
            // 2.1 查询其子L3基础数据
            List<CampaignExecuteInformationLv3Vo> lv3Infos = baseMapper.queryCampaignExecuteInformation4Lv3(id, lv2.getLv2ActivityId());
            if (CollectionUtils.isEmpty(lv3Infos)) {
                // 无L3数据
                lv2.setLv2RemainingAmountIncludeTax(lv2.getLv2ApplyAmountUsdIncludeTax());
                return;
            }

            lv3Infos.forEach(lv3 -> {
                lv3.setActivityExecutorUserCard(campaignCommonService.getUserByUserId(lv3.getActivityExecutorUserId()).getUserCard());
                lv3.setLv3Subject(activityTypeMap.get(lv3.getLv3Subject()));
                lv3.setLv3Status(isChinese ? ActivityStatusEnum.getStatusNameCnByCode(lv3.getLv3Status()) : ActivityStatusEnum.getStatusNameEnByCode(lv3.getLv3Status()));
                lv3.setPrStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(lv3.getPrStatusCode()) : PrPoStatusEnum.getNameEnByCode(lv3.getPrStatusCode()));
                lv3.setPoStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(lv3.getPoStatusCode()) : PrPoStatusEnum.getNameEnByCode(lv3.getPoStatusCode()));
                if(!isChinese) {
                    purchaseTypeDict.forEach(item -> {
                        if (StringUtils.equals(item.getValue(), lv3.getPurchaseType())) {
                            lv3.setPurchaseType(item.getValueEn());
                        }
                    });
                }
                if (StringUtils.isEmpty(lv3.getPrId())) {
                    return;
                }
                // 2.2 查询PO相关金额并设置金额值
                BigDecimal acceptancedAmountUsd = baseMapper.getL3AcceptancedAmountTotal(lv3.getPrId());
                lv3.setLv3AcceptancedAmountUsdIncludeTax(MathUtils.round(acceptancedAmountUsd, 2)); // l3已验收金额
                if (StringUtils.isNotEmpty(lv3.getPoId())) {
                    lv3.setPoAmount(baseMapper.getPoAmountTotal(lv3.getPoId())); // PO总金额
                    lv3.setPoAcceptanceAmount(baseMapper.getPoAcceptancedAmountTotal(lv3.getPrId(), lv3.getPoId())); // PO已验收金额
                    lv3.setPoUnAcceptancedAmount(MathUtils.subtract(lv3.getPoAmount(), lv3.getPoAcceptanceAmount())); // PO待验收金额
                    lv3.setPoBillingedAmount(baseMapper.getPoBillingedAmountTotal(lv3.getPoId())); // 累计开票金额
                    lv3.setPoPaymentedAmount(baseMapper.getPoPaymentedAmountTotal(lv3.getPoId())); // 累计付款金额
                }
                // 2.3 判断操作列是否可验收
//                if (CommonConstant.PURCHASE_TYPE_SELF_CN.equals(lv3.getPurchaseType())) {
//                    // PR验收
//                    if (userId.equals(lv3.getActivityExecutorUserId()) && null == lv3.getLv3AcceptancedAmountUsdIncludeTax()) {
//                        // 自行采购判断当前用户是否是验收人, 不用校验PR状态， 一位内自行采购PR一创建后就是关闭状态了
//                        lv3.setIsAccepted(CommonConstant.NORMAL);
//                    }
//                } else {
//                    // PO验收
//                    boolean isNormal4PoStatus = !PrPoStatusEnum.CLOSED.getCode().equalsIgnoreCase(lv3.getPoStatusCode()); // 非关闭的， 因为POstatus是定时任务执行的会有延时
//                    if (isNormal4PoStatus && (userId.equals(lv3.getActivityExecutorUserId())) && campareZero(lv3.getPoUnAcceptancedAmount()) == 1 && campareZero(lv3.getPoAmount()) == 1) {
//                        // 是否能够验收， Y能验收， N不能验收,（PO有效, 登录人=活动执行人， 剩余验收金额和数量大于0）
//                        lv3.setIsAccepted(CommonConstant.NORMAL);
//                    }
//                }
            });
            // 查询L2剩余可申请余额USD = L2申请金额 - L3申请金额汇总 + L3回冲金额 - L2已回冲金额
            Map<String, BigDecimal> amountMap= getAmountMap(lv3Infos);
            BigDecimal l3ApplyAmountUsdTotal = amountMap.get("l3ApplyAmountUsdTotal");
            BigDecimal l3RecoveryAmountUsdTotal = amountMap.get("l3RecoveryAmountUsdTotal");
            BigDecimal lv2RemainingAmountIncludeTax = MathUtils.subtract(MathUtils.add(MathUtils.subtract(lv2.getLv2ApplyAmountUsdIncludeTax(), l3ApplyAmountUsdTotal),l3RecoveryAmountUsdTotal), lv2.getLv2RecoveryAmounUsdIncludeTax());
            lv2.setLv2RemainingAmountIncludeTax(lv2RemainingAmountIncludeTax);

            // 查询L2验收金额USD = L3验收金额USD汇总
            lv2.setLv2ActuallyUsedAmountUsdIncludeTax(amountMap.get("lv3AcceptancedAmountUsdIncludeTaxTotal"));
            lv2.setActivityL3s(lv3Infos);
        });
        log.info("queryCampaignExecuteInformationHandle end campaignId:{}, id:{}", campaignId, id);
        return lv2Infos;
    }

    private Map<String, BigDecimal> getAmountMap(List<CampaignExecuteInformationLv3Vo> lv3Infos) {
        BigDecimal l3ApplyAmountUsdTotal = null;
        BigDecimal l3RecoveryAmountUsdTotal = null;
        BigDecimal lv3AcceptancedAmountUsdIncludeTaxTotal = null;
        // 同一个L3， 金额求和只取一次
        Set<String> l3ActivityIds = new HashSet<>();
        for (CampaignExecuteInformationLv3Vo lv3Info : lv3Infos) {
            if (l3ActivityIds.contains(lv3Info.getLv3ActivityId())) {
                continue;
            }
            l3ActivityIds.add(lv3Info.getLv3ActivityId());
            if (null != lv3Info.getLv3ApplyAmountUsdIncludeTax()) {
                l3ApplyAmountUsdTotal = MathUtils.add(lv3Info.getLv3ApplyAmountUsdIncludeTax(), l3ApplyAmountUsdTotal);
            }
            if (null != lv3Info.getLv3RecoveryAmounUsdIncludeTax()) {
                l3RecoveryAmountUsdTotal = MathUtils.add(lv3Info.getLv3RecoveryAmounUsdIncludeTax(), l3RecoveryAmountUsdTotal);
            }
            if (null != lv3Info.getLv3AcceptancedAmountUsdIncludeTax()) {
                lv3AcceptancedAmountUsdIncludeTaxTotal = MathUtils.add(lv3Info.getLv3AcceptancedAmountUsdIncludeTax(), lv3AcceptancedAmountUsdIncludeTaxTotal);
            }
        }

        Map<String, BigDecimal> amountMap = new HashMap<>();
        amountMap.put("l3ApplyAmountUsdTotal", l3ApplyAmountUsdTotal);
        amountMap.put("l3RecoveryAmountUsdTotal", l3RecoveryAmountUsdTotal);
        amountMap.put("lv3AcceptancedAmountUsdIncludeTaxTotal", lv3AcceptancedAmountUsdIncludeTaxTotal);
        return amountMap;
    }


    private int campareZero(BigDecimal amount) {
        if (null == amount) {
            // ＜ 0
            return -1;
        }
        return amount.compareTo(BigDecimal.ZERO);
    }

    /**
     * 列表导出--添加合并策略（EasyExcel）
     *
     * @param excelDtoList
     * @return
     */
    public static Map<String, List<RowRangeDto>> addMerStrategy(List<CampaignExecuteInformationExportCnVo> excelDtoList) {
        Map<String, List<RowRangeDto>> strategyMap = new HashMap<>();
        CampaignExecuteInformationExportCnVo preExcelDto = null;
        for (int i = 0; i < excelDtoList.size(); i++) {
            CampaignExecuteInformationExportCnVo currDto = excelDtoList.get(i);
            if (preExcelDto != null) {
                //从第二行开始判断是否需要合并
                if (StringUtils.isNotEmpty(currDto.getLv2ActivityId()) && StringUtils.isNotEmpty(preExcelDto.getLv2ActivityId()) && StringUtils.equals(currDto.getLv2ActivityId(),preExcelDto.getLv2ActivityId())) {
                    //如果Id一样，则可合并一列
                    for (int j = 0; j < 10; j++) {
                        fillStrategyMap(strategyMap, String.valueOf(j), i + 1);
                    }
                }
                if (StringUtils.isNotEmpty(currDto.getLv3ActivityId()) && StringUtils.isNotEmpty(preExcelDto.getLv3ActivityId()) && StringUtils.equals(currDto.getLv3ActivityId(),preExcelDto.getLv3ActivityId())) {
                    //如果Id一样，则可合并一列
                    for (int j = 10; j < 22; j++) {
                        fillStrategyMap(strategyMap, String.valueOf(j), i + 1);
                    }
                }
            }
            preExcelDto = currDto;
        }
        return strategyMap;
    }

    /**
     * 新增或修改合并策略map(EasyExcel)
     *
     * @param strategyMap
     * @param key
     * @param index
     */
    private static void fillStrategyMap(Map<String, List<RowRangeDto>> strategyMap, String key, int index) {
        List<RowRangeDto> rowRangeDtoList = strategyMap.get(key) == null ? new ArrayList<>() : strategyMap.get(key);
        boolean flag = false;
        for (RowRangeDto dto : rowRangeDtoList) {
            //分段list中是否有end索引是上一行索引的，如果有，则索引+1
            if (dto.getEnd() == index) {
                dto.setEnd(index + 1);
                flag = true;
            }
        }
        //如果没有，则新增分段
        if (!flag) {
            rowRangeDtoList.add(new RowRangeDto(index, index + 1));
        }
        strategyMap.put(key, rowRangeDtoList);
    }

    @Override
    public R getPoBillingedDetails(Map<String, Object> params) {
        List<PoBillingedAmountDetailVo> poBillingedAmountDetailVos = baseMapper.getPoBillingedDetails(params);
        return R.ok().put("data", poBillingedAmountDetailVos);
    }

    @Override
    public R getPoPaymentedDetails(Map<String, Object> params) {
        List<PoPaymentedAmountDetailVo> poBillingedAmountDetailVos = baseMapper.getPoPaymentedDetails(params);
        return R.ok().put("data", poBillingedAmountDetailVos);
    }

    @Override
    public R updateL2Recovery(Map<String, Object> params, SysUserEntity currentUser) {
        if (null == currentUser || null == currentUser.getUserId()) {
            return R.error("User not logged in");
        }
        String activityId = (String) params.get("activityId");
        Integer id = Integer.valueOf(params.get("id").toString());
        log.info("updateL2Recovery param id:{}, activityId:{}", id, activityId);
        BigDecimal l2ToRecoveryAmountUsd = new BigDecimal(params.get("l2ToRecoveryAmountUsd").toString());
        if (ZERO.compareTo(l2ToRecoveryAmountUsd) > 0) {
            log.error("updateL2Recovery recovery calculation error, Lv2 recovery amounnt less than 0, campaignId:{}, id:{}", activityId, id);
            return R.error(I18nUtil.getMessage("recoveryCalculationError"));
        }

        ActivityHistoryInfoVo l2InfoVo = activityHisInfoService.selectById(id);
        if (null == l2InfoVo) {
            return R.error("Data does not exist");
        }

        // LV2状态不是已启动， 不能关闭
        if (!ActivityStatusEnum.ACTIVATED.getStatusCode().equals(l2InfoVo.getStatus())) {
            return R.error(I18nUtil.getMessage("lv2CanNotClose"));
        }

        // LV2子LV3不是全部已关闭
        Wrapper<ActivityHistoryInfoVo> acWrapper = new EntityWrapper<>();
        acWrapper.eq("parent_id", activityId);
        acWrapper.eq("ref_id", l2InfoVo.getRefId());
        acWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> l3InfoVos = activityHisInfoService.selectList(acWrapper);
        for (ActivityHistoryInfoVo l3 : l3InfoVos) {
            if (!ActivityStatusEnum.CLOSED.getStatusCode().equals(l3.getStatus())) {
                return R.error(I18nUtil.getMessage("lv2CanNotCloseExistLv3"));
            }
        }

        // 保存Lv2回冲金额到数据库、更新L2状态为‘已关闭’
        l2InfoVo.setStatus(ActivityStatusEnum.CLOSED.getStatusCode());
        l2InfoVo.setRecoveryAmountUsd(l2ToRecoveryAmountUsd);
        l2InfoVo.setUpdatedTime(LocalDateTime.now());
        l2InfoVo.setUpdatedBy(currentUser.getUserId().toString());
        activityHisInfoService.updateById(l2InfoVo);
        return R.ok();
    }

    @Override
    public R getCampaignRecoveryDialogInfo(Map<String, Object> params, SysUserEntity currentUser) {
        if (null == currentUser || null == currentUser.getUserId()) {
            return R.error("User not logged in");
        }
        String campaignId = (String) params.get("campaignId");
        Integer id = Integer.valueOf(params.get("id").toString());
        log.info("getLv2RecoveryInfo param id:{}, campaignId:{}", id, campaignId);

        // 返回头数据即campaign数据、表格数据即L2数据
        CampaignHistoryInfoVo campaignVo = new CampaignHistoryInfoVo();
        List<ActivityHistoryInfoVo> l2InfoVos = new ArrayList<>();

        // 子LV2是不是全部已关闭
        Wrapper<ActivityHistoryInfoVo> ac2Wrapper = new EntityWrapper<>();
        ac2Wrapper.eq("level", 2);
        ac2Wrapper.eq("ref_id", id);
        ac2Wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        l2InfoVos = activityHisInfoService.selectList(ac2Wrapper);
        if (CollectionUtils.isEmpty(l2InfoVos)) {
            return R.ok().put("tableData", l2InfoVos).put("headData", campaignVo);
        }

        BigDecimal l2ApplyAmountUsdTotal = null; // L2申请金额USD汇总
        for (ActivityHistoryInfoVo l2 : l2InfoVos) {
            // Lv2申请金额USD
            BigDecimal applyAmountUsd = l2.getAmount().multiply(l2.getLatestRate());
            l2.setApplyAmountUsd(applyAmountUsd);
            l2ApplyAmountUsdTotal = MathUtils.add(applyAmountUsd, l2ApplyAmountUsdTotal);

            // l2累计已验收金额USD
            l2.setAcceptancedAmountUsd(MathUtils.subtract(applyAmountUsd, l2.getRecoveryAmountUsd()));
        }
        campaignVo.setApplyAmountUsd(l2ApplyAmountUsdTotal);

        // 子L3
        Wrapper<ActivityHistoryInfoVo> ac3Wrapper = new EntityWrapper<>();
        ac3Wrapper.eq("level", 3);
        ac3Wrapper.eq("ref_id", id);
        ac3Wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> l3InfoVos = activityHisInfoService.selectList(ac3Wrapper);

        BigDecimal l3AcceptancedAmountUsdTotal = null; // L3累计验收金额USD
        for (ActivityHistoryInfoVo l3 : l3InfoVos) {
            // 查询验收
            Wrapper<AcceptanceFormInfo> accWrapper = new EntityWrapper<>();
            accWrapper.eq("activity_id", l3.getActivityId());
            accWrapper.eq("status", AcceptanceFormStatusEnum.APPROVAL_SUCCESS.getStatusCode());
            accWrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
            List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceFormInfoService.selectList(accWrapper);
            BigDecimal acceptanceAmountUsd = acceptanceFormInfos.stream().map(item -> item.getAcceptanceAmount().multiply(item.getExchangeRate())).reduce(BigDecimal::add).orElse(null);
            if (null != acceptanceAmountUsd) {
                l3AcceptancedAmountUsdTotal = MathUtils.add(acceptanceAmountUsd, l3AcceptancedAmountUsdTotal);
            }
        }
        campaignVo.setAcceptancedAmountUsd(l3AcceptancedAmountUsdTotal);
        if (null != l3AcceptancedAmountUsdTotal) {
            // 待回冲金额
            campaignVo.setToRecoveryAmountUsd(MathUtils.subtract(l2ApplyAmountUsdTotal, l3AcceptancedAmountUsdTotal));
        }
        return R.ok().put("tableData", l2InfoVos).put("headData", campaignVo);
    }


    @Override
    public R updateCampaignRecovery(Map<String, Object> params, SysUserEntity currentUser) {
        if (null == currentUser || null == currentUser.getUserId()) {
            return R.error("User not logged in");
        }
        String campaignId = (String) params.get("campaignId");
        Integer id = Integer.valueOf(params.get("id").toString());
        log.info("updateCampaignRecovery param id:{}, campaignId:{}", id, campaignId);
        BigDecimal campaignToRecoveryAmountUsd = new BigDecimal(params.get("campaignToRecoveryAmountUsd").toString());
        if (ZERO.compareTo(campaignToRecoveryAmountUsd) > 0) {
            log.error("updateCampaignRecovery recovery calculation error, campaign recovery amounnt less than 0, campaignId:{}, id:{}", campaignId, id);
            return R.error(I18nUtil.getMessage("recoveryCalculationError"));
        }

        CampaignHistoryInfoVo campaignHistoryInfoVo = baseMapper.selectById(id);
        if (null == campaignHistoryInfoVo) {
            return R.error("Campaign Data does not exist");
        }

        // campaign状态不是已审批完， 不能关闭
        if (!CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode().equals(campaignHistoryInfoVo.getStatus())) {
            return R.error(I18nUtil.getMessage("campaignCanNotClose"));
        }

        // 子LV2是不是全部已关闭
        Wrapper<ActivityHistoryInfoVo> acWrapper = new EntityWrapper<>();
        acWrapper.eq("level", 2);
        acWrapper.eq("ref_id", campaignHistoryInfoVo.getId());
        acWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> l2InfoVos = activityHisInfoService.selectList(acWrapper);
        for (ActivityHistoryInfoVo l2 : l2InfoVos) {
            if (!ActivityStatusEnum.CLOSED.getStatusCode().equals(l2.getStatus())) {
                return R.error(I18nUtil.getMessage("campaignCanNotCloseExistLv2"));
            }
        }

        // 校验Campaign待回冲金额， 是否等于LV2回冲金额汇总
        BigDecimal lv2RecoveryAmountTotal = l2InfoVos.stream().map(ActivityHistoryInfoVo::getRecoveryAmountUsd).reduce(BigDecimal::add).orElse(ZERO);
        if (!MathUtils.round(lv2RecoveryAmountTotal, 2).equals(MathUtils.round(campaignToRecoveryAmountUsd, 2))) {
            // 校验campaign待回冲金额 是否等于 L2回冲金额USD汇总
            log.error("updateCampaignRecovery recovery calculation error, campaign recovery amounnt not equal Lv2 recovery amount, campaignId:{}, id:{}", campaignId, id);
            return R.error(I18nUtil.getMessage("recoveryCalculationError"));
        }

        // 保存Lv2回冲金额到数据库、更新L2状态为‘已关闭’
        campaignHistoryInfoVo.setStatus(CampaignStatusEnum.CLOSED.getStatusCode());
        campaignHistoryInfoVo.setRecoveryAmountUsd(campaignToRecoveryAmountUsd);
        campaignHistoryInfoVo.setUpdatedDate(new Date());
        campaignHistoryInfoVo.setUpdatedBy(currentUser.getUserId().toString());
        baseMapper.updateById(campaignHistoryInfoVo);
        return R.ok();
    }
}