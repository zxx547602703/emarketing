package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysDictService;
import com.framework.modules.sys.service.SysUserService;
import com.framework.modules.sys.shiro.ShiroUtils;
import com.wiko.emarket.constant.ActivityStatusEnum;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.constant.DeleteStatusEnum;
import com.wiko.emarket.dao.*;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.campaign.*;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.MyConstant;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ActivityHistoryInfoServiceImpl extends ServiceImpl<ActivityHistoryInfoDao, ActivityHistoryInfoVo> implements ActivityHistoryInfoService {
    @Autowired
    ActivityTypeService activityTypeService;

    @Autowired
    ExchangeService exchangeService;
    @Autowired
    SysUserService sysUserService;

    @Autowired
    BudgetTypeService budgetTypeService;

    @Autowired
    SysDictService sysDictService;

    @Autowired
    FinanceInfoService financeInfoService;

    @Autowired
    PurchaseInfoService purchaseInfoService;

    @Autowired
    BenefitProdService benefitProdService;

    @Autowired
    AttachmentInfoService attachmentInfoService;
    @Autowired
    private CompanyDao companyDao;
    @Autowired
    private PrInfoDao prInfoDao;
    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private CampaignHistoryInfoDao campaignHistoryInfoDao;

    @Autowired
    AreaService areaService;

    @Override
    public List<ActivityHistoryInfoVo> getActivityHisByCampaignId(String campaignId, Integer id) {
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        Wrapper<ActivityHistoryInfoVo> actL2Wrapper = new EntityWrapper<>();
        actL2Wrapper.eq("level", MyConstant.ACT_LV2_LEVEL);
        actL2Wrapper.eq("parent_id", campaignId);
        actL2Wrapper.eq("ref_id", id);
        actL2Wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> actL2List = selectList(actL2Wrapper);

        // TODO
        Wrapper<ActivityHistoryInfoVo> actL3Wrapper = new EntityWrapper<>();
        actL3Wrapper.eq("level", MyConstant.ACT_LV3_LEVEL);
        actL3Wrapper.in("parent_id", actL2List.stream().map(map -> map.getActivityId()).collect(Collectors.toList()));
        actL3Wrapper.eq("ref_id", id);
        actL3Wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> actL3List = selectList(actL3Wrapper);

        // 获取字典列表
        List<SysDictEntity> purchaseType = sysDictService.findByType("purchaseType");
        List<SysDictEntity> acceptanceType = sysDictService.findByType("acceptanceType");

        // 获取用户列表
        List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
        Map<Long, String> userMap = sysUserEntities.stream().collect(Collectors.toMap(SysUserEntity::getUserId, SysUserEntity::getUserCard));

        List<AreaEntity> areaEntities = areaService.selectList(null);
        Map<String, String> areaMap = areaEntities.stream().collect(Collectors.toMap(AreaEntity::getCode, isChinese ?
                AreaEntity::getNameCn : AreaEntity::getNameEn, (k1, k2) -> k1));

        // 查询业务科目列表
        List<ActivityType> activityTypes = activityTypeService.selectList(new EntityWrapper<>());
        actL2List.stream().forEach(a -> {
            // 查询L3列表
            List<ActivityHistoryInfoVo> act3 = new ArrayList<>();

            // 根据业务科目code查询nameCn
            activityTypes.stream().forEach(at -> {
                if (at.getCode().toString().equals(a.getLv1Subject())) {
                    a.setLv1SubjectLabel(isChinese ? at.getNameCn() : at.getNameEn());
                }
                if (at.getCode().toString().equals(a.getLv2Subject())) {
                    a.setLv2SubjectLabel(isChinese ? at.getNameCn() : at.getNameEn());
                }
            });

            actL3List.stream().forEach(a3 -> {
                if (a.getActivityId().equals(a3.getParentId())) {
                    a3.setCtyCodeLabel(areaMap.get(a3.getCtyCode()));

                    // 查询L3附件列表
                    Wrapper<AttachmentInfo> attachWrapper = new EntityWrapper<>();
                    attachWrapper.eq("ref_id", a3.getId());
                    attachWrapper.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
                    List<AttachmentInfo> attachmentInfoVos = attachmentInfoService.selectList(attachWrapper);
                    a3.setAttachments(attachmentInfoVos);
                    a3.setAttachmentsName(attachmentInfoVos.stream().map(map -> map.getAttachmentName()).collect(Collectors.joining(",")));

                    // 查询财经信息
                    FinanceInfo financeInfo = financeInfoService.getFinanceByActId(a3.getActivityId(), a3.getId().toString());
                    a3.setFinanceInfo(financeInfo);
                    // 根据支付中心code查询支付中心和公司名称返回
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("code",a3.getCenterId());
                    CostCenterVO costCenterVO = companyDao.queryCostCenterInfo(map);
                    a3.setCenterName(costCenterVO.getDescription());
                    a3.setPaymentCompanyName(costCenterVO.getCompanyName());
                    HashMap<String, Object> activityIdMap = new HashMap<>();
                    activityIdMap.put("activityId",a3.getActivityId());
                    CampaignPrDetailsVO prDetail = campaignDao.getPrDetails(activityIdMap);
                    if(prDetail!=null && prDetail.getPrId()!=null){
                        a3.setPrId(prDetail.getPrId());
                    }
                    // 查询收益产品
                    a3.setProducts(benefitProdService.getProdByActId(a3.getActivityId(), a3.getId()));
                    a3.setLv1SubjectLabel(a.getLv1SubjectLabel());
                    a3.setLv2SubjectLabel(a.getLv2SubjectLabel());
                    // 根据业务科目code查询nameCn
                    activityTypes.stream().forEach(at -> {
                        if (at.getCode().toString().equals(a3.getLv3Subject())) {
                            a3.setLv3SubjectLabel(isChinese ? at.getNameCn() : at.getNameEn());
                        }
                    });
                    // 查询采购信息
                    PurchaseInfo purchaseByActId = purchaseInfoService.getPurchaseByActId(a3.getActivityId(), a3.getId().toString());
                    if(purchaseByActId != null){
                        if(!isChinese){
                            purchaseType.stream().forEach(d -> {
                                if(purchaseByActId.getPurchaseType().equals(d.getValue())){
                                    purchaseByActId.setPurchaseType(d.getValueEn());
                                }
                            });
                            acceptanceType.stream().forEach(d -> {
                                if(purchaseByActId.getAcceptanceType().equals(d.getValue())){
                                    purchaseByActId.setAcceptanceType(d.getValueEn());
                                }
                            });
                        }
                        purchaseByActId.setPurchaseSubject(Strings.isBlank(a3.getLv3SubjectLabel()) ? purchaseByActId.getPurchaseSubject():a3.getLv3SubjectLabel());
                    }
                    a3.setPurchaseInfo(purchaseByActId);

                    if (Strings.isNotBlank(a3.getActivityExecutor())) {
                        a3.setActivityExecutorLabel(userMap.get(Long.valueOf(a3.getActivityExecutor())));
                    }
                    act3.add(a3);
                }
            });

            // 查询附件列表
            Wrapper<AttachmentInfo> attachWrapper = new EntityWrapper<>();
            attachWrapper.eq("ref_id", a.getId());
            attachWrapper.eq("status", MyConstant.DEL_STAUTS_NORMAL);
            List<AttachmentInfo> attachmentInfoVos = attachmentInfoService.selectList(attachWrapper);
            a.setAttachments(attachmentInfoVos);
            a.setAttachmentsName(attachmentInfoVos.stream().map(map -> map.getAttachmentName()).collect(Collectors.joining(",")));

            // 设置相关金额值
            setAmountValue(act3, a);

            a.setActivityL3s(act3);
        });
        return actL2List;
    }

    /**
     * 设置金额
     *
     * @param l3Infos
     * @param l2Vo
     */
    private void setAmountValue(List<ActivityHistoryInfoVo> l3Infos, ActivityHistoryInfoVo l2Vo) {
        l2Vo.setApplyAmountUsd(MathUtils.multiply(l2Vo.getAmount(), l2Vo.getLatestRate()));
        if (CollectionUtils.isEmpty(l3Infos)) {
            l2Vo.setRemainingAmountUsd(l2Vo.getApplyAmountUsd());
            return;
        }

        BigDecimal l3AcceptancedAmountUsdTotal = null; // L3累计验收金额USD
        BigDecimal l3ApplyAmountUsdTotal = null; // L3申请金额USD汇总
        BigDecimal l3RecoveyAmountUsdTotal = null; // L3回冲金额USD汇总
        for (ActivityHistoryInfoVo l3 : l3Infos) {
            if (null != l3.getFinanceInfo() && null != l3.getFinanceInfo().getApplyAmountUsd()) {
                l3ApplyAmountUsdTotal = MathUtils.add(l3.getFinanceInfo().getApplyAmountUsd(), l3ApplyAmountUsdTotal);
            }
            if (null != l3.getRecoveryAmountUsd()) {
                l3RecoveyAmountUsdTotal = MathUtils.add(l3.getRecoveryAmountUsd(), l3RecoveyAmountUsdTotal);
            }
            if (StringUtils.isNotEmpty(l3.getPrId())) {
                BigDecimal acceptancedAmountUsd = campaignHistoryInfoDao.getL3AcceptancedAmountTotal(l3.getPrId());
                l3.setAcceptancedAmountUsd(MathUtils.round(acceptancedAmountUsd, 2));
                if (null != acceptancedAmountUsd) {
                    // 用四舍五入后的值参与计算， 表头上的值和表中的汇总值能保持一致
                    l3AcceptancedAmountUsdTotal = MathUtils.add(l3.getAcceptancedAmountUsd(),l3AcceptancedAmountUsdTotal);
                }
            }
        }

        // L2剩余可拆分金额USD = L2申请金额USD -L3申请金额USD汇总 + L3回冲金额USD汇总 - L2回冲金额USD
        BigDecimal l2RemainingAmountUsd = MathUtils.subtract(MathUtils.add(MathUtils.subtract(l2Vo.getApplyAmountUsd(), l3ApplyAmountUsdTotal), l3RecoveyAmountUsdTotal), l2Vo.getRecoveryAmountUsd());

        l2Vo.setRemainingAmountUsd(l2RemainingAmountUsd);
        // L2 已验收金额 = L3已验收金额汇总
        l2Vo.setAcceptancedAmountUsd(l3AcceptancedAmountUsdTotal);
    }

    @Transactional
    @Override
    public R updateInfo(CampaignCreateVo info, String action, boolean isAddRecord) {
        log.info("campaign update activity Info:{}", info);
        if (info.getActivityInfos() == null) {
            return R.ok();
        }
        // 查询campaign下的LV2列表
        Wrapper<ActivityHistoryInfoVo> act2s = new EntityWrapper<>();
        act2s.eq("level", MyConstant.ACT_LV2_LEVEL);
        act2s.eq("ref_id", info.getId());
        act2s.eq("parent_id", info.getCampaignId());
        act2s.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<ActivityHistoryInfoVo> dbActivityInfos = selectList(act2s);

        // 删除已保存的记录
        dbActivityInfos.stream()
                .filter(item -> !info.getActivityInfos().stream()
                        .map(e -> e.getActivityId())
                        .collect(Collectors.toList())
                        .contains(item.getActivityId()))
                .collect(Collectors.toList()).stream().forEach(a -> {
                    a.setDelStatus(MyConstant.DEL_STATUS_DELETED);
                    insertOrUpdate(a);
                });

        // 查询LV2下的LV3列表
        if (dbActivityInfos.size() > 0) {
            Wrapper<ActivityHistoryInfoVo> actL3Wrapper = new EntityWrapper<>();
            actL3Wrapper.eq("level", MyConstant.ACT_LV3_LEVEL);
            actL3Wrapper.eq("ref_id", info.getId());
            actL3Wrapper.in("parent_id", dbActivityInfos.stream().map(map -> map.getActivityId()).collect(Collectors.toList()));
            actL3Wrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
            List<ActivityHistoryInfoVo> dbActL3s = selectList(actL3Wrapper);

            // 删除已保存的记录
            info.getActivityInfos().stream().forEach(a2 -> {
                dbActL3s.stream()
                        .filter(item -> !a2.getActivityL3s().stream()
                                .map(e -> e.getActivityId())
                                .collect(Collectors.toList())
                                .contains(item.getActivityId()))
                        .collect(Collectors.toList()).stream().forEach(a -> {
                            a.setDelStatus(MyConstant.DEL_STATUS_DELETED);
                            insertOrUpdate(a);
                        });
            });
        }

        batchInsertOrUpdateAct(info, action, isAddRecord);
        return R.ok();
    }

    @Override
    public void updateActivityStatusByCampaignId(String campaignId, Integer id, String status, String deleteStatusCode) {
        Wrapper<ActivityHistoryInfoVo> actWrapper = new EntityWrapper<>();
        actWrapper.eq("parent_id", campaignId);
        actWrapper.eq("ref_id", id);
        actWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());

        List<ActivityHistoryInfoVo> actL2List = selectList(actWrapper);

        Long userId = ShiroUtils.getUserId();
        actL2List.forEach(a2 -> {
            a2.setStatus(status);
            a2.setDelStatus(deleteStatusCode);
            a2.setUpdatedBy(userId.toString());
            a2.setUpdatedTime(LocalDateTime.now());
            updateById(a2);
        });
    }

    /**
     * 批量插入ActivityInfo
     *
     * @param info
     * @return
     */
    public void batchInsertOrUpdateAct(CampaignCreateVo info, String operateType, boolean isAddRecord) {
        LocalDateTime now = LocalDateTime.now();
        String operator = ((SysUserEntity) SecurityUtils.getSubject().getPrincipal()).getUserId().toString();

        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        List<SysDictEntity> purchaseType = sysDictService.findByType("purchaseType");
        List<SysDictEntity> acceptanceType = sysDictService.findByType("acceptanceType");

        // 取所有币种的最新汇率
        List<ExchangeRateInfoVo> rates = exchangeService.getLastestRateList();
        Date submitDate = new Date();
        info.getActivityInfos().stream().forEach(a -> {
            // 保存或更新ActivityL2
            a.setLevel(MyConstant.ACT_LV2_LEVEL);
            if (Strings.isBlank(a.getActivityId())) {
                a.setActivityId(getActivityId(a.getLevel()).toString());
            }

            // 查看是否有历史通过的版本
            setActivityStatus(info.getClickType(), a);
            a.setParentId(info.getCampaignId());
            a.setActivityExecutor(" ");
            a.setCtyCode(" ");
            a.setDelStatus(MyConstant.DEL_STAUTS_NORMAL);
            if (isAddRecord || a.getCreatedTime() == null || StringUtils.isEmpty(a.getCreatedBy())) {
                // 只要是新增就更新创建时间和创建人
                a.setCreatedBy(operator);
                a.setCreatedTime(now);
            }

            a.setUpdatedTime(now);
            a.setUpdatedBy(operator);
            ActivityHistoryInfoVo lv2ActivityHistoryInfoVo = new ActivityHistoryInfoVo();
            BeanUtils.copyProperties(a, lv2ActivityHistoryInfoVo);
            if (CommonConstant.OPERATE_TYPE_SUBMIT.equals(operateType)) {
                // 只要是提交， 设置当前提交时间
                lv2ActivityHistoryInfoVo.setSubmitDate(new Date());
            }
            // 设置lv2的初始汇率、最新汇率
            BigDecimal latestRateVal = getLatestRate(a.getCurrency(), rates);
            BigDecimal initRate = a.getInitRate() == null ? latestRateVal : a.getInitRate();
            BigDecimal latestRate = a.getLatestRate() == null ? latestRateVal : a.getLatestRate();
            lv2ActivityHistoryInfoVo.setInitRate(initRate);
            lv2ActivityHistoryInfoVo.setLatestRate(latestRate);
            lv2ActivityHistoryInfoVo.setRefId(info.getId());
            lv2ActivityHistoryInfoVo.setProcessId(info.getProcessId());
            lv2ActivityHistoryInfoVo.setSubmitDate(submitDate);

            if (!isAddRecord) {
                insertOrUpdate(lv2ActivityHistoryInfoVo);
            } else {
                insert(lv2ActivityHistoryInfoVo);
            }
            a.setId(lv2ActivityHistoryInfoVo.getId());

            // 保存或更新ActivityL3
            if (a.getActivityL3s() != null) {
                a.getActivityL3s().stream().forEach(a3 -> {
                    a3.setParentId(a.getActivityId());
                    if (Strings.isBlank(a3.getActivityId())) {
                        a3.setActivityId(getActivityId(a3.getLevel()).toString());
                    }
                    if ("0".equals(info.getClickType())) {
                        // 保存， 如果回冲时PR关闭导致L3关闭， 那么再次提交L3时状态不变, 因为已关闭是终态
                        if (StringUtils.equals(a3.getStatus(), ActivityStatusEnum.CLOSED.getStatusCode())) {
                            a3.setStatus(a3.getStatus());
                        } else {
                            a3.setStatus(Strings.isNotBlank(a3.getStatus()) ? a3.getStatus() : ActivityStatusEnum.DRAFT.getStatusCode());
                        }
                    } else {
                        if (StringUtils.equals(a3.getStatus(), ActivityStatusEnum.CLOSED.getStatusCode())) {
                            a3.setStatus(a3.getStatus());
                        } else {
                            a3.setStatus(ActivityStatusEnum.ACTIVATED.getStatusCode());
                        }
                    }
//                    a3.setPaymentCompany(" ");
                    a3.setLevel(MyConstant.ACT_LV3_LEVEL);
                    a3.setLv1Subject(a.getLv1Subject());
                    a3.setLv2Subject(a.getLv2Subject());
                    a3.setDelStatus(MyConstant.DEL_STAUTS_NORMAL);
                    if (isAddRecord || a3.getCreatedTime() == null || StringUtils.isEmpty(a3.getCreatedBy())) {
                        // 只要是新增就更新创建时间和创建人
                        a3.setCreatedBy(operator);
                        a3.setCreatedTime(now);
                    }
                    a3.setUpdatedTime(now);
                    a3.setUpdatedBy(operator);
                    a3.setCtyCode(info.getBudgetBelongCountryCode());
                    ActivityHistoryInfoVo lv3ActivityHistoryInfoVo = new ActivityHistoryInfoVo();
                    BeanUtils.copyProperties(a3, lv3ActivityHistoryInfoVo);

                    if (CommonConstant.OPERATE_TYPE_SUBMIT.equals(operateType)) {
                        // 只要是提交（包括驳回再提交）， 设置当前提交时间
                        lv3ActivityHistoryInfoVo.setSubmitDate(new Date());
                    }
                    // 设置lv3的初始汇率、最新汇率
                    // 设置lv2的初始汇率、最新汇率
                    BigDecimal latestRateL3Val = getLatestRate(a3.getFinanceInfo().getCurrency(), rates);
                    BigDecimal initRateL3 = a3.getInitRate() == null ? latestRateL3Val : a3.getInitRate();
                    BigDecimal latestRateL3 = a3.getLatestRate() == null ? latestRateL3Val : a3.getLatestRate();
                    lv3ActivityHistoryInfoVo.setInitRate(initRateL3);
                    lv3ActivityHistoryInfoVo.setLatestRate(latestRateL3);
                    lv3ActivityHistoryInfoVo.setRefId(info.getId());
                    lv3ActivityHistoryInfoVo.setProcessId(info.getProcessId());
                    lv3ActivityHistoryInfoVo.setSubmitDate(submitDate);
                    // 初次编辑/保存/提交等操作， 都保存'实际使用金额'， 因为拆分按钮显示逻辑和提交金额校验， 会使用该字段值
                    lv3ActivityHistoryInfoVo.setActuallyUsedAmountUsd(a3.getActuallyUsedAmountUsd());

                    if (!isAddRecord) {
                        insertOrUpdate(lv3ActivityHistoryInfoVo);
                    } else {
                        insert(lv3ActivityHistoryInfoVo);
                    }
                    a3.setId(lv3ActivityHistoryInfoVo.getId());

                    // 保存L3附件信息， 逻辑同L2附件
                    saveLv2Attachment(a3, operator, isAddRecord);

                    // 保存或更新财经信息
                    saveFinance(a3, a, operator, isAddRecord, now);
                    
                    // 保存或更新采购信息
                    savePurchase(a3, operator, isAddRecord, now, isChinese, purchaseType, acceptanceType);

                    // 保存或更新受益产品
                    saveBenefitProd(a3, operator, isAddRecord);
                });
            }
            // 保存LV2附件
            saveLv2Attachment(a, operator, isAddRecord);
        });
    }

    private void setActivityStatus(String clickType, ActivityHistoryInfoVo a) {
        Wrapper<ActivityHistoryInfoVo> acWrapper = new EntityWrapper<>();
        acWrapper.ne("id", a.getId());
        acWrapper.eq("activity_id", a.getActivityId());
        acWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        acWrapper.orderBy("id", false);
        List<ActivityHistoryInfoVo> dbActivityInfos = baseMapper.selectList(acWrapper);
        if (CollectionUtils.isNotEmpty(dbActivityInfos)) {
            // 上个版本是什么状态， 这个版本状态不变
            a.setStatus(dbActivityInfos.get(0).getStatus());
        } else {
            // 点击保存
            if ("0".equals(clickType)) {
                a.setStatus(Strings.isNotBlank(a.getStatus()) ? a.getStatus() : ActivityStatusEnum.DRAFT.getStatusCode());
            } else {
                // 活动状态为草稿和提交状态更新活动状态
                if (Strings.isBlank(a.getStatus()) || ActivityStatusEnum.DRAFT.getStatusCode().equals(a.getStatus()) ||
                        ActivityStatusEnum.SUBMITTED.getStatusCode().equals(a.getStatus())) {
                    if ("0".equals(clickType)) {
                        a.setStatus(ActivityStatusEnum.DRAFT.getStatusCode());
                    } else {
                        // 拆分Lv3状态为已启动
                        a.setStatus(a.getActivityL3s() != null && a.getActivityL3s().size() > 0 ? ActivityStatusEnum.ACTIVATED.getStatusCode() :
                                ActivityStatusEnum.SUBMITTED.getStatusCode());
                    }
                }
            }
        }
    }

    private void saveLv2Attachment(ActivityHistoryInfoVo lv2Vo, String operator, boolean isAddRecord) {
        // 更新状态删除其他
        Wrapper<AttachmentInfo> attachWrapper = new EntityWrapper<>();
        attachWrapper.eq("ref_id", lv2Vo.getId());
        List<AttachmentInfo> attachmentInfoVos = attachmentInfoService.selectList(attachWrapper);
        attachmentInfoVos.stream()
                .filter(item -> !lv2Vo.getAttachments().stream()
                        .map(e -> e.getAttachmentId())
                        .collect(Collectors.toList())
                        .contains(item.getAttachmentId()))
                .collect(Collectors.toList()).stream().forEach(at -> {
                    at.setStatus(MyConstant.DEL_STATUS_DELETED);
                    Wrapper<AttachmentInfo> atWrapper = new EntityWrapper<>();
                    atWrapper.eq("attachment_id", at.getAttachmentId());
                    attachmentInfoService.update(at, atWrapper);
                });

        if (CollectionUtils.isEmpty(lv2Vo.getAttachments())) {
            return;
        }
        // 保存或更新文件
        lv2Vo.getAttachments().stream().forEach(am -> {
            if (isAddRecord) {
                // 非编辑场景， 新增记录
                am.setAttachmentId(null);
            }
            am.setRefId(lv2Vo.getId().toString());
            am.setCreatedBy(operator);
            am.setStatus(MyConstant.DEL_STAUTS_NORMAL);
            am.setCreatedDate(new Date());
        });

        attachmentInfoService.insertOrUpdateBatch(lv2Vo.getAttachments());
    }

    private void saveBenefitProd(ActivityHistoryInfoVo lv3Vo, String operator, boolean isAddRecord) {
        Wrapper<BenefitProd> prodWrapper = new EntityWrapper<>();
        prodWrapper.eq("ref_id", lv3Vo.getId());
        prodWrapper.eq("activity_id", lv3Vo.getActivityId());
        prodWrapper.eq("status", MyConstant.DEL_STAUTS_NORMAL);
        List<BenefitProd> benefitProds = benefitProdService.selectList(prodWrapper);

        if (lv3Vo.getProducts() != null) {
            benefitProds.stream()
                    .filter(item -> !lv3Vo.getProducts().stream()
                            .map(e -> e.getUuid())
                            .collect(Collectors.toList())
                            .contains(item.getUuid()))
                    .collect(Collectors.toList()).stream().forEach(bp -> {
                        bp.setStatus(MyConstant.DEL_STATUS_DELETED);
                        Wrapper<BenefitProd> pWrapper = new EntityWrapper<>();
                        pWrapper.eq("uuid", bp.getUuid());
                        benefitProdService.update(bp, pWrapper);
                    });
            lv3Vo.getProducts().stream().forEach(p -> {
                if (!isAddRecord) {
                    p.setUuid(Strings.isNotBlank(p.getUuid()) ? p.getUuid() : UUID.randomUUID().toString().replace("-", ""));
                    if (p.getCreatedDate() == null) {
                        p.setCreatedBy(operator);
                        p.setCreatedDate(new Date());
                    }
                } else {
                    p.setUuid(UUID.randomUUID().toString().replace("-", ""));
                    p.setCreatedBy(operator);
                    p.setCreatedDate(new Date());
                }
                p.setProdName(p.getProdId());
                p.setActivityId(lv3Vo.getActivityId());
                p.setStatus(MyConstant.DEL_STAUTS_NORMAL);
                p.setUpdatedBy(operator);
                p.setUpdatedDate(new Date());
                p.setRefId(lv3Vo.getId().toString());
                benefitProdService.insertOrUpdate(p);
            });
        } else {
            benefitProds.forEach(bp -> {
                bp.setStatus(MyConstant.DEL_STATUS_DELETED);
                Wrapper<BenefitProd> pWrapper = new EntityWrapper<>();
                pWrapper.eq("uuid", bp.getUuid());
                benefitProdService.update(bp, pWrapper);
            });
        }
    }

    private void savePurchase(ActivityHistoryInfoVo lv3Vo, String operator, boolean isAddRecord, LocalDateTime now, boolean isChinese, List<SysDictEntity> purchaseType, List<SysDictEntity> acceptanceType) {
        if (lv3Vo.getPurchaseInfo() == null) {
            return;
        }
        PurchaseInfo purchaseInfo = lv3Vo.getPurchaseInfo();
        if (!isAddRecord) {
            purchaseInfo.setUuid(Strings.isNotBlank(purchaseInfo.getUuid()) ? purchaseInfo.getUuid() : UUID.randomUUID().toString().replace("-", ""));
        } else {
            purchaseInfo.setUuid(UUID.randomUUID().toString().replace("-", ""));
        }
        purchaseInfo.setUuid(Strings.isNotBlank(purchaseInfo.getUuid()) ? purchaseInfo.getUuid() : UUID.randomUUID().toString().replace("-", ""));
        purchaseInfo.setActivityId(lv3Vo.getActivityId());
        purchaseInfo.setPurchaseSubject(Strings.isBlank(purchaseInfo.getPurchaseSubject()) ? " " : purchaseInfo.getPurchaseSubject());
        purchaseInfo.setStatus(MyConstant.DEL_STAUTS_NORMAL);
        if (purchaseInfo.getCreatedTime() == null) {
            purchaseInfo.setCreatedBy(operator);
            purchaseInfo.setCreatedTime(now);
        }
        purchaseInfo.setUpdatedBy(operator);
        purchaseInfo.setUpdatedTime(now);
        if(!isChinese){
            purchaseType.stream().forEach(d -> {
                if(purchaseInfo.getPurchaseType().equals(d.getValueEn())){
                    purchaseInfo.setPurchaseType(d.getValue());
                }
            });
            acceptanceType.stream().forEach(d -> {
                if(purchaseInfo.getAcceptanceType().equals(d.getValueEn())){
                    purchaseInfo.setAcceptanceType(d.getValue());
                }
            });
        }
        purchaseInfo.setRefId(lv3Vo.getId().toString());
        purchaseInfoService.insertOrUpdate(purchaseInfo);
    }

    private void saveFinance(ActivityHistoryInfoVo lv3Vo, ActivityHistoryInfoVo lv2Vo, String operator, boolean isAddRecord, LocalDateTime now) {
        // 保存或更新财经信息
        if (lv3Vo.getFinanceInfo() == null) {
            return;
        }

        FinanceInfo financeInfo = lv3Vo.getFinanceInfo();
        if (!isAddRecord) {
            financeInfo.setUuid(Strings.isNotBlank(financeInfo.getUuid()) ? financeInfo.getUuid() : UUID.randomUUID().toString().replace("-", ""));
        } else {
            financeInfo.setUuid(UUID.randomUUID().toString().replace("-", ""));
        }

        financeInfo.setUuid(Strings.isNotBlank(financeInfo.getUuid()) ? financeInfo.getUuid() : UUID.randomUUID().toString().replace("-", ""));
        financeInfo.setSource(lv2Vo.getSource());
//      financeInfo.setPaymentCompany(a.getPaymentCompany());
        financeInfo.setActivityId(lv3Vo.getActivityId());
//      financeInfo.setApplyAmountUsd(financeInfo.getApplyAmountUsd() == null ? BigDecimal.ZERO : financeInfo.getApplyAmountUsd());
        financeInfo.setStatus(MyConstant.DEL_STAUTS_NORMAL);
        financeInfo.setCurrency(Strings.isBlank(financeInfo.getCurrency()) ? lv2Vo.getCurrency() : financeInfo.getCurrency());
        if (financeInfo.getCreatedTime() == null) {
            financeInfo.setCreatedBy(operator);
            financeInfo.setCreatedTime(now);
        }
        financeInfo.setUpdatedBy(operator);
        financeInfo.setUpdatedTime(now);
        financeInfo.setRefId(lv3Vo.getId().toString());
        financeInfoService.insertOrUpdate(financeInfo);
    }


    /**
     * 生成activityId
     *
     * @param level
     * @return
     */
    public StringBuilder getActivityId(String level) {
        Wrapper<ActivityHistoryInfoVo> wrapper = new EntityWrapper<>();
//        wrapper.eq("level", level);
        wrapper.gt("created_time", LocalDateTime.parse(LocalDate.now().toString() + "T00:00:00"));
        wrapper.lt("created_time", LocalDateTime.parse(LocalDate.now().toString() + "T23:59:59"));
        wrapper.orderBy("activity_id", false);
        wrapper.last("limit 1");
        ActivityHistoryInfoVo activityInfo = selectOne(wrapper);

        StringBuilder activityId = new StringBuilder();
        if (activityInfo == null) {
            activityId = activityId.append("A").append(LocalDate.now().toString().replace("-", "").substring(2)).append("001");
        } else {
            activityId = increaseActId(activityId.append(activityInfo.getActivityId()));
        }
        return activityId;
    }

    public StringBuilder increaseActId(StringBuilder sb) {
        String num = sb.substring(7);// sb.substring(2)去掉前两个字符
        int num1 = Integer.parseInt(num);
        num1++;
        String str = String.format("%03d", num1);// 如果小于6位左边补0
        String ret = sb.substring(0, 7) + str;
        return new StringBuilder(ret);
    }

//    private BigDecimal getInitRate(String activityId, String currency) {
//        Wrapper<ActivityHistoryInfoVo> wrapper = new EntityWrapper<>();
//        wrapper.eq("activity_id", activityId);
//        wrapper.notIn("del_status", DeleteStatusEnum.DELETED.getStatusCode());
//        wrapper.orderBy("created_time", false);
//        wrapper.last("limit 1");
//        ActivityHistoryInfoVo dbActivityHisVo = selectOne(wrapper);
//
//        Date initRateDate = dbActivityHisVo == null ? new Date() : dbActivityHisVo.getSubmitDate();
//        Map<String,Object> initParam = new HashMap<>();
//        initParam.put("fromCurrency", currency);
//        initParam.put("rateDate", CommonDateUtil.toYYYYMMStrFormat(initRateDate));
//        // 初始汇率
//        BigDecimal initRate = exchangeService.queryExchange(initParam);
//
//        return initRate;
//    }
//
    private BigDecimal getLatestRate(String currency, List<ExchangeRateInfoVo> rates) {
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
}