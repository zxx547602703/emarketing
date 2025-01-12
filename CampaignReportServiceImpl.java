package com.wiko.emarket.service.campaign.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.utils.Dates;
import com.framework.common.utils.PageUtil;
import com.framework.common.utils.PageUtils;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysDictService;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.*;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.acceptance.AcceptanceFormInfoService;
import com.wiko.emarket.service.acceptance.PrInfoService;
import com.wiko.emarket.service.campaign.*;
import com.wiko.emarket.service.campaign.ActivityHistoryInfoService;
import com.wiko.emarket.service.campaign.CampaignHistoryInfoService;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.util.RowRangeDto;
import com.wiko.emarket.vo.*;
import com.wiko.emarket.vo.marketingBudget.AmountVO;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
@Slf4j
public class CampaignReportServiceImpl implements CampaignReportService {
    @Autowired
    private CountryMarketingBudgetDao marketingBudgetDao;

    @Autowired
    private BudgetDao budgetDao;

    @Autowired
    private BudgetTypeDao budgetTypeDao;

    @Autowired
    private AreaDao areaDao;

    @Autowired
    private AttachmentInfoDao attachmentInfoDao;

    @Autowired
    private SysUserAreaDao sysUserAreaDao;

    @Autowired
    private ActivityTypeDao activityTypeDao;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    PurchaseInfoService purchaseInfoService;

    @Autowired
    FinanceInfoService financeInfoService;

    @Autowired
    BenefitProdService benefitProdService;

    @Autowired
    CampaignService campaignService;

    @Autowired
    private CampaignHistoryInfoService campaignHistoryInfoService;

    @Autowired
    private ActivityHistoryInfoService activityHistoryInfoService;

    @Autowired
    SysDictService sysDictService;

    @Autowired
    private AcceptanceFormInfoService acceptanceFormInfoService;

    @Autowired
    private CampaignHistoryInfoDao campaignHistoryInfoDao;

    @Autowired
    private PrInfoService prInfoService;

    @Override
    public PageUtils getCampaignBudgetList(Map<String, Object> params, Long userId) {
        int page = Integer.parseInt(params.get("currentPage").toString());
        int size = Integer.parseInt(params.get("pagesize").toString());
        // 获取所有数据
        params.put("language", CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())?CommonConstant.EN_US_LANGUAGE:CommonConstant.ZH_CN_LANGUAGE);
        List<CampaignBudgetVo> campaginBudgetVoList = getCampaignBudgetVos(params);

        // 根据用户权限过滤数据
        List<CampaignBudgetVo> filterVos = getBelongBudgetDatas(campaginBudgetVoList, userId);
        // 分页返回
        return new PageUtils(PageUtil.limit(filterVos, size, page), filterVos.size(), size, page);
    }

    @Override
    public void exportCampaignBudget(Map<String, Object> params, Long userId, HttpServletResponse response) throws IOException {
        //查询数据
        List<CampaignBudgetVo> allDatas = getCampaignBudgetVos(params);
        // 根据用户权限过滤数据
        List<CampaignBudgetVo> filterVos = getBelongBudgetDatas(allDatas, userId);
        String title = "Campagin预算明细";
        String fileName = title + Dates.getDateString();
        //调用接口导出表格
        if(CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))){
            title = "Campaign Budget Breakdown";
            CommonsUtils.exportExcel(title, fileName, filterVos, CampaignBudgetEnVo.class, response);
        }
        else {
            CommonsUtils.exportExcel(title, fileName, filterVos, CampaignBudgetVo.class, response);
        }
    }

    @Override
    public PageUtils getCampaignExecuteList(Map<String, Object> params, Long userId) {
        // page,size
        int page = Integer.parseInt(params.get("currentPage").toString());
        int size = Integer.parseInt(params.get("pagesize").toString());
        params.put("language", CommonConstant.EN_LANGUAGE.equals(RequestUtil.getLang())?CommonConstant.EN_US_LANGUAGE:CommonConstant.ZH_CN_LANGUAGE);
        // 查询数据
        List<CampaignHistoryInfoVo> campaignInfoVoList = getCampaignExecuteDetails(params);

        // 根据用户权限(区域过滤)过滤数据
        List<CampaignHistoryInfoVo> filterVos = getBelongExecuteDatas(campaignInfoVoList, userId);

        // 返回数据
        return new PageUtils(PageUtil.limit(filterVos, size, page), filterVos.size(), size, page);
    }

    @Override
    public void exportCampaignExecute(Map<String, Object> params, Long userId, HttpServletResponse response) throws IOException {
        //查询数据
        long time1 = System.currentTimeMillis();
        log.info("getCampaignExecuteDetails start");
        if(params.get("budgetTypes") != null){
            params.put("budgetTypes", Strings.isNotBlank(params.get("budgetTypes").toString()) ? Arrays.asList(params.get("budgetTypes").toString().split(",")) : new ArrayList<>());
        }
        Boolean isExecute =  Boolean.parseBoolean(params.get("isExecute").toString()) ;

        List<CampaignHistoryInfoVo> campaignInfoVoList = getCampaignExecuteDetails(params);
        long time2 = System.currentTimeMillis();
        log.info("getCampaignExecuteDetails end =====>{}", time2 - time1);

        // 根据用户权限过滤数据, 我的campaign导出不用区域过滤
        List<CampaignHistoryInfoVo> filterVos = campaignInfoVoList;
        if(!"my".equals(params.get("type"))){
            filterVos = getBelongExecuteDatas(campaignInfoVoList, userId);
        }
        long time3 = System.currentTimeMillis();
        log.info("getBelongExecuteDatas end =====>{}", time3 - time2);

        List<CampaignExecuteExportVo> exportVos = new ArrayList<>();
        // 组装CampaignExecuteExportVo
        for (CampaignHistoryInfoVo item : filterVos) {
            List<ActivityHistoryInfoVo> ActivityInfosLv2 = item.getActivityInfos();
            if (CollectionUtils.isEmpty(ActivityInfosLv2)) {
                CampaignExecuteExportVo exportVo = new CampaignExecuteExportVo();
                BeanUtils.copyProperties(item, exportVo);
                exportVos.add(exportVo);
                continue;
            }
            for (ActivityHistoryInfoVo lv2Vo : ActivityInfosLv2) {
                List<ActivityHistoryInfoVo> activityInfosLv3 = lv2Vo.getActivityL3s();
                if (CollectionUtils.isEmpty(activityInfosLv3)) {
                    CampaignExecuteExportVo exportVo = new CampaignExecuteExportVo();
                    BeanUtils.copyProperties(item, exportVo);
                    // 组装LV2Vo
                    buildExpoertVoLv2(lv2Vo, exportVo);
                    exportVos.add(exportVo);
                } else {
                    activityInfosLv3.forEach(lv3Vo -> {
                        CampaignExecuteExportVo exportVo = new CampaignExecuteExportVo();
                        BeanUtils.copyProperties(item, exportVo);
                        // 组装LV2Vo
                        buildExpoertVoLv2(lv2Vo, exportVo);
                        // 组装LV3Vo
                        buildExpoertVoLv3(lv3Vo, exportVo);
                        exportVos.add(exportVo);
                    });

                }
            }
        }
        long time4 = System.currentTimeMillis();
        log.info("build exportVos end =====>{}", time4 - time3);
        String title = isExecute?"Campaign明细执行清单":"campaign明细查询清单";

        String fileName = title + Dates.getDateString();

        // 要合并的单元格数据
        Map<String, List<RowRangeDto>> strategyMap = addMerStrategy(exportVos);
        long time5 = System.currentTimeMillis();
        log.info("addMerStrategy end =====>{}", time5 - time4);
        if(CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))){
            title = "Campaign execution details";
            CommonsUtils.exportExcel(title, fileName, exportVos, CampaignExecuteExportEnVo.class, response, strategyMap, 2);
        }
        else{
            CommonsUtils.exportExcel(title, fileName, exportVos, CampaignExecuteExportVo.class, response, strategyMap, 2);
        }
        //调用接口导出表格
        long time6 = System.currentTimeMillis();
        log.info("exportExcel end =====>{}", time6 - time5);
    }

    private void buildExpoertVoLv3(ActivityHistoryInfoVo lv3Vo, CampaignExecuteExportVo exportVo) {
        exportVo.setLv3Id(lv3Vo.getActivityId());
        exportVo.setLv3Status(lv3Vo.getStatus());
        exportVo.setLv3Description(lv3Vo.getDescription());
        exportVo.setLv3Subject(lv3Vo.getLv3Subject());
        exportVo.setLv3StartDate(lv3Vo.getStartDate());
        exportVo.setLv3EndDate(lv3Vo.getEndDate());
        exportVo.setLv3ActivityExecutor(lv3Vo.getActivityExecutor());
        exportVo.setLv3AreaName(lv3Vo.getAreaName());

        // 财经信息
        FinanceInfo financeInfo = lv3Vo.getFinanceInfo();
        if (null != financeInfo) {
            exportVo.setLv3Currency(financeInfo.getCurrency());
            exportVo.setLv3ApplyAmount(financeInfo.getApplyAmount());
            exportVo.setLv3ApplyAmountUsd(financeInfo.getApplyAmountUsd());
        }
        // 采购信息
        PurchaseInfo purchaseInfo = lv3Vo.getPurchaseInfo();
        if (null != purchaseInfo) {
            exportVo.setLv3PurchaseType(purchaseInfo.getPurchaseType());
            exportVo.setLv3CompletedDate(purchaseInfo.getCompletedDate());
            exportVo.setLv3PurchaseSubject(purchaseInfo.getPurchaseSubject());
            exportVo.setLv3AcceptanceType(purchaseInfo.getAcceptanceType());
            exportVo.setLv3Address(purchaseInfo.getAddress());
        }
        exportVo.setLv3AcceptancedAmountUsd(lv3Vo.getAcceptancedAmountUsd());
        exportVo.setLv3RecoveryAmountUsd(lv3Vo.getRecoveryAmountUsd());
        exportVo.setLv3InitRate(lv3Vo.getInitRate());
        exportVo.setLv3LatestRate(lv3Vo.getLatestRate());

        exportVo.setLv3CreatedBy(lv3Vo.getCreatedBy());
        exportVo.setLv3CreatedDate(lv3Vo.getCreatedTime());
        exportVo.setLv3UpdatedBy(lv3Vo.getUpdatedBy());
        exportVo.setLv3UpdatedDate(lv3Vo.getUpdatedTime());

    }

    private void buildExpoertVoLv2(ActivityHistoryInfoVo lv2Vo, CampaignExecuteExportVo exportVo) {
        exportVo.setLv2Id(lv2Vo.getActivityId());
        exportVo.setLv2Status(lv2Vo.getStatus());
        exportVo.setLv2Description(lv2Vo.getDescription());
        exportVo.setLv1Subject(lv2Vo.getLv1Subject());
        exportVo.setLv2Subject(lv2Vo.getLv2Subject());
        exportVo.setLv2StartDate(lv2Vo.getStartDate());
        exportVo.setLv2EndDate(lv2Vo.getEndDate());
        exportVo.setLv2Source(lv2Vo.getSource());
        exportVo.setLv2Currency(lv2Vo.getCurrency());
        exportVo.setLv2Amount(lv2Vo.getAmount());
        exportVo.setLv2ApplyAmountUsd(lv2Vo.getApplyAmountUsd());
        exportVo.setLv2RemainingAmountUsd(lv2Vo.getRemainingAmountUsd());
        exportVo.setLv2RecoveryAmountUsd(lv2Vo.getRecoveryAmountUsd());
        exportVo.setLv2InitRate(lv2Vo.getInitRate());
        exportVo.setLv2LatestRate(lv2Vo.getLatestRate());
        exportVo.setLv2CreatedBy(lv2Vo.getCreatedBy());
        exportVo.setLv2CreatedDate(lv2Vo.getCreatedTime());
        exportVo.setLv2UpdatedBy(lv2Vo.getUpdatedBy());
        exportVo.setLv2UpdatedDate(lv2Vo.getUpdatedTime());
    }

    private List<CampaignBudgetVo> getCampaignBudgetVos(Map<String, Object> params) {
        if (null != params.get("country") && "" != params.get("country")) {
            // 国家(代表处)不为空， 就不需要根据地区部门查询了
            params.put("region", null);
        }
        // 查询某年、某地区的预算表总记录
        List<CampaignBudgetVo> campaginBudgetVoList = marketingBudgetDao.selectListGroupBy(params);
        // 去除重复记录
        campaginBudgetVoList = campaginBudgetVoList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getYear() + ";" + o.getAreaCode() + ";" + o.getSource()))), ArrayList::new));

        // 国家地区总记录
        List<AreaEntity> areaVoList = areaDao.selectList(null);
        Map<String, String> areaMap = new HashMap<>();
        if(CommonConstant.EN_US_LANGUAGE.equals(params.get("language"))){
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameEn()));
        }
        else{
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameCn()));
        }
        Map<String, String> parentCodeMap = new HashMap<>();
        areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !parentCodeMap.containsKey(areaVo.getCode())).forEach(areaVo -> parentCodeMap.put(areaVo.getCode(), areaVo.getParentId()));

        //  查询已启动、已提交Activiti总记录
        EntityWrapper<ActivityHistoryInfoVo> activityWrapper = new EntityWrapper<>();
        activityWrapper.notIn("status",  Arrays.asList(ActivityStatusEnum.DRAFT.getStatusCode())).andNew();
        activityWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());

        for (CampaignBudgetVo vo : campaginBudgetVoList) {
            // 设置币种
            vo.setCurrency(CommonConstant.USD);
            vo.setParentCode(parentCodeMap.get(vo.getAreaCode()));
            vo.setBudgetAmount(MathUtils.round(vo.getBudgetAmount(), CommonConstant.SCALE));

            // 设置收益金额
            BigDecimal startedIncomeAmount = getStartedIncomeAmount(vo);
            vo.setStartedIncomeAmount(MathUtils.round(startedIncomeAmount, CommonConstant.SCALE));

            // 设置预算剩余
            BigDecimal budgetSurplus = MathUtils.subtract(vo.getBudgetAmount(), vo.getStartedIncomeAmount());
            vo.setBudgetSurplus(MathUtils.round(budgetSurplus, CommonConstant.SCALE));

            // 设置全球、地区、国家名称
            String areaName = areaMap.get(vo.getAreaCode());
            vo.setRegion(areaName);
        }
        // 重新组装排序
        return sortCampaignBudgetVos(campaginBudgetVoList);
    }

    private List<CampaignBudgetVo> sortCampaignBudgetVos(List<CampaignBudgetVo> campaginBudgetVoList) {
        List<CampaignBudgetVo> returnVos = new ArrayList<>();
        List<String> years = campaginBudgetVoList.stream().map(CampaignBudgetVo::getYear).distinct().sorted().collect(Collectors.toList());
        for (String year : years){
            List<CampaignBudgetVo> yearBudgetVos = campaginBudgetVoList.stream().filter(item -> StringUtils.equals(item.getYear(), year)).collect(Collectors.toList());
            // 单独找到全球
            List<CampaignBudgetVo> wordWideInfos = yearBudgetVos.stream().filter(item -> StringUtils.equals(item.getLevel(), AreaLevelEnum.WORLDWIDE.getLevel())).collect(Collectors.toList());
            returnVos.addAll(wordWideInfos);
            // 找到HQ地区部
            List<CampaignBudgetVo> hqRegionInfos = yearBudgetVos.stream().filter(item -> StringUtils.equals(item.getLevel(), AreaLevelEnum.REGION.getLevel()) && item.getAreaCode().contains("HQ")).collect(Collectors.toList());
            returnVos.addAll(hqRegionInfos);
            for (CampaignBudgetVo hq : hqRegionInfos){
                // 找其下的代表处， 无论是HQ(MBF)、HQ(MSF)其子都一样的，所以只查询一次
                List<CampaignBudgetVo> hqRepresentInfos = yearBudgetVos.stream().filter(item -> StringUtils.equals(item.getLevel(), AreaLevelEnum.REPRESENT_OFFICE.getLevel()) && StringUtils.equals(item.getParentCode(), hq.getAreaCode()) ).collect(Collectors.toList());
                returnVos.addAll(hqRepresentInfos);
                break;
            }
            // 找到其它地区部及其下代表处
            List<CampaignBudgetVo> otherRegionInfos = yearBudgetVos.stream().filter(item -> StringUtils.equals(item.getLevel(), AreaLevelEnum.REGION.getLevel()) && !item.getAreaCode().contains("HQ")).collect(Collectors.toList());
            for (CampaignBudgetVo other : otherRegionInfos){
                returnVos.add(other);
                // 找其下的代表处
                List<CampaignBudgetVo> otherRepresentInfos = yearBudgetVos.stream().filter(item -> StringUtils.equals(item.getParentCode(), other.getAreaCode()) ).collect(Collectors.toList());
                returnVos.addAll(otherRepresentInfos);
            }
            // 添加单独的办事处, 因为没有父级， 上面无法找出
            returnVos.addAll(listCompare(returnVos, yearBudgetVos));
        }
        Collections.reverse(returnVos);
        return returnVos;
    }

    private List<CampaignBudgetVo> listCompare(List<CampaignBudgetVo> destList, List<CampaignBudgetVo> sourceList) {
        Map<String,Boolean> map = new HashMap<>();
        List<CampaignBudgetVo> differentList = new ArrayList<>();
        for(CampaignBudgetVo dest : destList){
            StringBuilder sb = new StringBuilder();
            sb.append(dest.getYear()).append(dest.getAreaCode()).append(dest.getLevel()).append(dest.getSource());
            map.put(sb.toString(), true);
        }
        for(CampaignBudgetVo source : sourceList){
            StringBuilder sb = new StringBuilder();
            sb.append(source.getYear()).append(source.getAreaCode()).append(source.getLevel()).append(source.getSource());
            if(map.get(sb.toString()) == null){
                differentList.add(source);
            }
        }
        return differentList;
    }

    private List<CampaignHistoryInfoVo> getCampaignExecuteDetails(Map<String, Object> params) {
        // 用户map
        List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
        Map<String, String> userMap = sysUserEntities.stream().collect(Collectors.toMap(sysUserEntity -> String.valueOf(sysUserEntity.getUserId()), SysUserEntity::getUserCard, (a, b) -> b));

        List<CampaignHistoryInfoVo> campaignInfoVoList = new ArrayList<>();
        if(Objects.equals(params.get("exportType"), "campaignQueryExport")){
            // 查询campaign的导出
            List<String> status = params.get("campaignStatus") != null && StringUtils.isNotEmpty(params.get("campaignStatus").toString()) ? Arrays.asList(params.get("campaignStatus").toString()) : new ArrayList<>();
            params.put("status", status);
        } else {
            // 报表的导出
            List<String> status = params.get("campaignStatus") != null && StringUtils.isNotEmpty(params.get("campaignStatus").toString()) ? CampaignStatusEnum.getStatusCodesByName(params.get("campaignStatus").toString()) : new ArrayList<>();
            params.put("status", status);
        }

        List<CampaignHistoryInfoVo> list = campaignHistoryInfoService.findList(params);

        campaignInfoVoList = JSON.parseArray(JSON.toJSONString(list), CampaignHistoryInfoVo.class);

        if (CollectionUtils.isEmpty(campaignInfoVoList)) {
            return campaignInfoVoList;
        }

        // budget表按条件查询记录
        List<BudgetTypeEntity> budgetTypeVoList = budgetTypeDao.selectList(null);
        Map<String, String> budgetMap = new HashMap<>();
        boolean isEnglish = CommonConstant.EN_US_LANGUAGE.equals(params.get("language"));
        if (isEnglish) {
            budgetTypeVoList.stream().filter(budget -> budget.getCode() != null && !budgetMap.containsKey(budget.getCode())).forEach(budget -> budgetMap.put(budget.getCode(), budget.getNameEn()));
        } else {
            budgetTypeVoList.stream().filter(budget -> budget.getCode() != null && !budgetMap.containsKey(budget.getCode())).forEach(budget -> budgetMap.put(budget.getCode(), budget.getNameCn()));
        }

        // activity表查询总纪录, 创建时间降序排序
        Wrapper<ActivityHistoryInfoVo> actWrapper = new EntityWrapper<>();
        actWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
        actWrapper.orderBy("created_time", false);
        List<ActivityHistoryInfoVo> activityVoList = activityHistoryInfoService.selectList(actWrapper);

        // 查询附件信息总记录
        List<AttachmentInfo> AttachmentInfoList = attachmentInfoDao.selectList(null);

        // 国家地区总记录
        List<AreaEntity> areaVoList = areaDao.selectList(null);
        Map<String, String> areaMap = new HashMap<>();
        if (isEnglish) {
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameEn()));
        } else {
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameCn()));
        }
        // 科目类型
        List<ActivityType> activityTypeVoList = activityTypeDao.selectList(null);
        Map<String, String> activityTypeMap = new HashMap<>();
        if (isEnglish) {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameEn()));
        } else {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameCn()));
        }
        // 设置其下的childs
        for (CampaignHistoryInfoVo item : campaignInfoVoList) {
            // 查询 budgetType，状态字段成name
            item.setBudgetType(budgetMap.get(item.getBudgetType()));
//            budgetTypeVoList.stream().filter(typeVo -> StringUtils.equals(item.getBudgetType(), typeVo.getCode())).findFirst().ifPresent(typeVo -> item.setBudgetType(typeVo.getNameCn()));
            item.setBudgetBelongAreaName(areaMap.get(item.getBudgetBelongAreaCode())); // 一级预算组织（地区部）N            item.setCountry(areaMap.get(item.getBudgetBelongCountryCode())); // 国家
            item.setRepresentativeName(areaMap.get(item.getRepresentative())); // 二级预算组织（代表处）
            item.setBudgetBelongCountryName(areaMap.get(item.getBudgetBelongCountryCode()));
            item.setStatus(isEnglish ? CampaignStatusEnum.getStatusNameEnByCode(item.getStatus()) : CampaignStatusEnum.getStatusNameCnByCode(item.getStatus())); // 状态
            item.setCampaignPa(userMap.get(item.getCampaignPa()));
            item.setCampaignOwner(userMap.get(item.getCampaignOwner()));
            item.setCreatedBy(userMap.get(item.getCreatedBy()));
            item.setUpdatedBy(userMap.get(item.getUpdatedBy()));
            String isValidVersion = item.getIsValidVersion().equals(CommonConstant.VALID_VERISON_EN) ?
                    (isEnglish ? CommonConstant.VALID_VERISON_EN : CommonConstant.VALID_VERISON_CN) :
                    (isEnglish ? CommonConstant.INVALID_VERISON_EN : CommonConstant.INVALID_VERISON_CN);
            item.setIsValidVersion(isValidVersion);
            // 过滤附件信息
            List<AttachmentInfo> attachmentInfos = AttachmentInfoList.stream().filter(AttachmentInfo -> StringUtils.equals(AttachmentInfo.getRefId(), item.getId().toString())).collect(Collectors.toList());
            item.setAttachmentInfos(attachmentInfos);

            // 过滤campaign其下的activityLV2记录
            List<ActivityHistoryInfoVo> activityLv2VoList = filter2ActivityLv2s(activityVoList, item, activityTypeMap, userMap,isEnglish);
            if (CollectionUtils.isEmpty(activityLv2VoList)) {
                item.setActivityInfos(null);
                continue;
            }
            item.setActivityInfos(activityLv2VoList);
            // 过滤activityLV2其下的activityLV3记录
            activityLv2VoList.forEach(lv2Vo -> {
                List<ActivityHistoryInfoVo> activityLv3VoList = filter2ActivityLv3s(activityVoList, item, lv2Vo, activityTypeMap, userMap,isEnglish);
                // 设置金额
                if (Objects.equals(params.get("exportType"), "campaignQueryExport")){
                    // 只有导出才需要查询金额
                    setAmountValue(activityLv3VoList, lv2Vo);
                }
                if (CollectionUtils.isEmpty(activityLv3VoList)) {
                    lv2Vo.setActivityL3s(null);
                    return;
                }
                lv2Vo.setActivityL3s(activityLv3VoList);
            });
            if (Objects.equals(params.get("exportType"), "campaignQueryExport")){
                // 只有导出才需要查询金额
                setCamapignAmount(item);
            }
        }
        return campaignInfoVoList;
    }

    /**
     * 设置金额
     *
     * @param l3Infos
     * @param l2Vo
     */
    public void setAmountValue(List<ActivityHistoryInfoVo> l3Infos, ActivityHistoryInfoVo l2Vo) {
        l2Vo.setApplyAmountUsd(MathUtils.round(MathUtils.multiply(l2Vo.getAmount(), l2Vo.getLatestRate()), 2));
        if (CollectionUtils.isEmpty(l3Infos)) {
            l2Vo.setRemainingAmountUsd(l2Vo.getApplyAmountUsd());
            return;
        }
        // 如果L2已关闭就赋值回冲金额， 非关闭就计算剩余可用金额
        if (ActivityStatusEnum.CLOSED.getStatusCode().equals(l2Vo.getStatus())) {
            l2Vo.setRemainingAmountUsd(l2Vo.getRecoveryAmountUsd());
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

            // 查询关联的PR
            Wrapper<PrInfoEntity> prWrapper = new EntityWrapper<>();
            prWrapper.eq("ref_activity_id", l3.getId());
            prWrapper.last("limit 1");
            PrInfoEntity prInfoEntity = prInfoService.selectOne(prWrapper);
            if (null != prInfoEntity && StringUtils.isNotEmpty(prInfoEntity.getPrId())) {
                BigDecimal acceptancedAmount = campaignHistoryInfoDao.getL3AcceptancedAmountTotal(prInfoEntity.getPrId());
                l3.setAcceptancedAmountUsd(MathUtils.round(acceptancedAmount, 2));
                if (null != acceptancedAmount) {
                    l3AcceptancedAmountUsdTotal = MathUtils.add(l3.getAcceptancedAmountUsd(), l3AcceptancedAmountUsdTotal);
                }
            }
        }

        // L2剩余可用金额USD = L2申请金额USD -L3申请金额USD汇总 + L3回冲金额USD汇总 +l2回冲金额USD
        BigDecimal l2RemainingAmountUsd = MathUtils.add(MathUtils.add(MathUtils.subtract(l2Vo.getApplyAmountUsd(), l3ApplyAmountUsdTotal), l3RecoveyAmountUsdTotal), l2Vo.getRecoveryAmountUsd());
        l2Vo.setRemainingAmountUsd(l2RemainingAmountUsd);
    }

    private void transferCreatedByToUid(Map<String, Object> params, List<SysUserEntity> sysUserEntities){
        String createdBy = (String)params.get("createdBy");
        if (StringUtils.isEmpty(createdBy)) {
            return;
        }
        sysUserEntities.stream().filter(sysUserEntity -> StringUtils.equals(sysUserEntity.getUsername(), createdBy))
                .map(sysUserEntity -> null == sysUserEntity.getUserId() ? "" : sysUserEntity.getUserId().toString())
                .findFirst().ifPresent(userId -> params.put("createdBy", userId));
    }

    private List<ActivityHistoryInfoVo> filter2ActivityLv2s(List<ActivityHistoryInfoVo> allActivityVos, CampaignHistoryInfoVo campaignInfo, Map<String, String> activityTypeMap, Map<String, String> userMap,boolean isEnglish) {
        List<ActivityHistoryInfoVo> activityLv2VoList = new ArrayList<>();
        for (ActivityHistoryInfoVo acti2 : allActivityVos) {
            if (StringUtils.equals(campaignInfo.getCampaignId(), acti2.getParentId()) && campaignInfo.getId().equals(acti2.getRefId())) {
                acti2.setStatus(isEnglish ? ActivityStatusEnum.getStatusNameEnByCode(acti2.getStatus()) : ActivityStatusEnum.getStatusNameCnByCode(acti2.getStatus()));
                String Lv1Subject = activityTypeMap.get(acti2.getLv1Subject()) == null ? acti2.getLv1Subject() : activityTypeMap.get(acti2.getLv1Subject());
                String Lv2Subject = activityTypeMap.get(acti2.getLv2Subject()) == null ? acti2.getLv2Subject() : activityTypeMap.get(acti2.getLv2Subject());
                acti2.setLv1Subject(Lv1Subject);
                acti2.setLv2Subject(Lv2Subject);
                acti2.setCreatedBy(userMap.get(acti2.getCreatedBy()));
                acti2.setUpdatedBy(userMap.get(acti2.getUpdatedBy()));
                activityLv2VoList.add(acti2);
            }
        }
        return activityLv2VoList;
    }

    private List<ActivityHistoryInfoVo> filter2ActivityLv3s(List<ActivityHistoryInfoVo> allActivityVos, CampaignHistoryInfoVo campaignInfo, ActivityHistoryInfoVo lv2Info, Map<String, String> activityTypeMap, Map<String, String> userMap,boolean isEnglish) {
        List<ActivityHistoryInfoVo> activityLv3VoList = new ArrayList<>();
        List<SysDictEntity> purchaseType = sysDictService.findByType("purchaseType");
        List<SysDictEntity> acceptanceType = sysDictService.findByType("acceptanceType");
        for (ActivityHistoryInfoVo acti3 : allActivityVos) {
            if (StringUtils.equals(lv2Info.getActivityId(), acti3.getParentId()) && campaignInfo.getId().equals(acti3.getRefId())) {
                acti3.setStatus(isEnglish?ActivityStatusEnum.getStatusNameEnByCode(acti3.getStatus()):ActivityStatusEnum.getStatusNameCnByCode(acti3.getStatus()));
                String Lv3Subject = activityTypeMap.get(acti3.getLv3Subject()) == null ? acti3.getLv3Subject() : activityTypeMap.get(acti3.getLv3Subject());
                acti3.setLv3Subject(Lv3Subject);
                acti3.setCurrency(lv2Info.getCurrency());
                acti3.setActivityExecutor(userMap.get(acti3.getActivityExecutor()));
                acti3.setCreatedBy(userMap.get(acti3.getCreatedBy()));
                acti3.setUpdatedBy(userMap.get(acti3.getUpdatedBy()));
                // 查询采购信息
                PurchaseInfo purchaseByActId = purchaseInfoService.getPurchaseByActId(acti3.getActivityId(), acti3.getId().toString());
                if(purchaseByActId != null){
                    if(isEnglish){
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
                    purchaseByActId.setPurchaseSubject(acti3.getLv3Subject());
                }
                acti3.setPurchaseInfo(purchaseByActId);
                // 查询财经信息
                acti3.setFinanceInfo(financeInfoService.getFinanceByActId(acti3.getActivityId(), String.valueOf(acti3.getId())));
                // 查询收益产品
                acti3.setProducts(benefitProdService.getProdByActId(acti3.getActivityId(), acti3.getProcessId()));
                activityLv3VoList.add(acti3);
            }
        }
        return activityLv3VoList;
    }

    // 获取收益金额
    private BigDecimal getStartedIncomeAmount(CampaignBudgetVo vo) {
        HashMap<String, String> map = new HashMap<>();
        map.put("year", vo.getYear());
        map.put("level", vo.getLevel());
        map.put("source", vo.getSource());
        // 全球不用设值
        if (AreaLevelEnum.REGION.getLevel().equals(vo.getLevel())) {
            map.put("areaCode", vo.getAreaCode());
        } else if (AreaLevelEnum.REPRESENT_OFFICE.getLevel().equals(vo.getLevel())) {
            map.put("representative", vo.getAreaCode());
        }

        // 查询国家地区已收益金额
        List<AmountVO> amountVOS = budgetDao.queryAmountRate(map);
        BigDecimal benefitAmount = BigDecimal.ZERO;
        for (AmountVO amountVO : amountVOS) {
            if (amountVO.getAmount() != null && amountVO.getSaveLatestRate() != null) {
                if (CampaignStatusEnum.CLOSED.getStatusCode().equals(amountVO.getCampaignStatus())) {
                    // cmapaign已关闭， 回冲金额回预算池, 否则不回
                    benefitAmount = benefitAmount.add(MathUtils.subtract(amountVO.getAmount().multiply(amountVO.getSaveLatestRate()), amountVO.getRecoveryAmountUsd()));
                } else {
                    benefitAmount = benefitAmount.add(amountVO.getAmount().multiply(amountVO.getSaveLatestRate()));
                }
            }
        }
        return benefitAmount;
    }

    /**
     * 过滤得到用户归属区域的数据
     *
     * @param campaginBudgetVoList 未过滤前的Campaign预算数据
     * @param userId 用户id
     * @return 过滤后得到的数据
     */
    private List<CampaignBudgetVo> getBelongBudgetDatas(List<CampaignBudgetVo> campaginBudgetVoList, Long userId){
        Map<String, Set<String>> areaMap = getBelogAreaCodes(userId);
        Set<String> lv1Set = areaMap.get("lv1AreaCodes");
        Set<String> lv2Set = areaMap.get("lv2AreaCodes");
        Set<String> lv3Set = areaMap.get("lv3AreaCodes");
        if (CollectionUtils.isNotEmpty(lv1Set)) {
            return campaginBudgetVoList;
        }

        if (CollectionUtils.isNotEmpty(lv2Set)) {
            lv2Set.addAll(lv3Set);
            return campaginBudgetVoList.stream().filter(vo -> lv2Set.contains(vo.getAreaCode())).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(lv3Set)) {
            return campaginBudgetVoList.stream().filter(vo -> lv3Set.contains(vo.getAreaCode())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 过滤得到用户归属区域的数据
     *
     * @param campaignInfoVos 未过滤前的Campaign执行明细数据
     * @param userId 用户id
     * @return 过滤后得到的数据
     */
    private List<CampaignHistoryInfoVo> getBelongExecuteDatas(List<CampaignHistoryInfoVo> campaignInfoVos, Long userId){
        Map<String, Set<String>> areaMap = getBelogAreaCodes(userId);
        Set<String> lv1Set = areaMap.get("lv1AreaCodes");
        Set<String> lv2Set = areaMap.get("lv2AreaCodes");
        Set<String> lv3Set = areaMap.get("lv3AreaCodes");
        if (CollectionUtils.isNotEmpty(lv1Set)) {
            return campaignInfoVos;
        }

        if (CollectionUtils.isNotEmpty(lv2Set)) {
            lv2Set.addAll(lv3Set);
            return campaignInfoVos.stream().filter(vo -> lv2Set.contains(vo.getBudgetBelongAreaCode())).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(lv3Set)) {
            return campaignInfoVos.stream().filter(vo -> lv3Set.contains(vo.getRepresentative())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Map<String, Set<String>> getBelogAreaCodes(Long userId) {
        EntityWrapper wrapper = new EntityWrapper();
        wrapper.eq("user_id",userId);
        List<SysUserArea> sysList = sysUserAreaDao.selectList(wrapper);
        Set<String> lv1Set = new HashSet<>();
        Set<String> lv2Set = new HashSet<>();
        Set<String> lv3Set = new HashSet<>();
        for(SysUserArea sysUserArea:sysList){
            if(AreaLevelEnum.WORLDWIDE.getLevel().equals(sysUserArea.getLevel())){
                lv1Set.add(sysUserArea.getAreaCode());
                break;
            }
            if(AreaLevelEnum.REGION.getLevel().equals(sysUserArea.getLevel())){
                lv2Set.add(sysUserArea.getAreaCode());
                continue;
            }
            if(AreaLevelEnum.REPRESENT_OFFICE.getLevel().equals(sysUserArea.getLevel())){
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
     * 列表导出--添加合并策略（EasyExcel）
     *
     * @param excelDtoList
     * @return
     */
    public static Map<String, List<RowRangeDto>> addMerStrategy(List<CampaignExecuteExportVo> excelDtoList) {
        Map<String, List<RowRangeDto>> strategyMap = new HashMap<>();
        CampaignExecuteExportVo preExcelDto = null;
        for (int i = 0; i < excelDtoList.size(); i++) {
            CampaignExecuteExportVo currDto = excelDtoList.get(i);
            if (preExcelDto != null) {
                //从第二行开始判断是否需要合并
                if (StringUtils.isNotEmpty(currDto.getCampaignId()) && StringUtils.isNotEmpty(preExcelDto.getCampaignId()) && StringUtils.equals(currDto.getCampaignId(),preExcelDto.getCampaignId())) {
                    //如果Id一样，则可合并一列
                    for (int j = 0; j < 22; j++) {
                        fillStrategyMap(strategyMap, String.valueOf(j), i + 1);
                    }
                }
                if (StringUtils.isNotEmpty(currDto.getLv2Id()) && StringUtils.isNotEmpty(preExcelDto.getLv2Id()) && StringUtils.equals(currDto.getLv2Id(),preExcelDto.getLv2Id())) {
                    //如果Id一样，则可合并一列
                    for (int j = 22; j < 41; j++) {
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
        campaignHistoryInfoVo.setApplyAmountUsd(MathUtils.round(applyAmountTotal, 2));

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
        campaignHistoryInfoVo.setAcceptancedAmountUsd(MathUtils.round(acceptancedAmountTotal, 2));
    }
}
