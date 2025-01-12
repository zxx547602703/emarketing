package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysDictEntity;
import com.framework.modules.sys.service.SysDictService;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.BudgetDao;
import com.wiko.emarket.dao.CampaignDao;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.entity.CountryMarketingBudgetEntity;
import com.wiko.emarket.entity.CountryQuarterBudgetEntity;
import com.wiko.emarket.entity.FinanceInfo;
import com.wiko.emarket.service.campaign.*;
import com.wiko.emarket.util.I18nUtil;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.MyConstant;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.ActivityHistoryInfoVo;
import com.wiko.emarket.vo.CampaignCreateVo;
import com.wiko.emarket.vo.CampaignHistoryInfoVo;
import com.wiko.emarket.vo.ExchangeRateInfoVo;
import com.wiko.emarket.vo.marketingBudget.AmountVO;
import com.wiko.psi.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ActivityInfoServiceImpl implements ActivityInfoService {
    @Autowired
    PurchaseInfoService purchaseInfoService;

    @Autowired
    FinanceInfoService financeInfoService;

    @Autowired
    CountryMarketingBudgetService marketingBudgetService;

    @Autowired
    CountryQuarterBudgetService countryQuarterBudgetService;

    @Autowired
    CampaignService campaignService;

    @Autowired
    AreaService areaService;

    @Autowired
    BenefitProdService benefitProdService;

    @Autowired
    AttachmentInfoService attachmentInfoService;

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
    private BudgetDao budgetDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private CampaignHistoryInfoService campaignHistoryInfoService;

    @Autowired
    private ActivityHistoryInfoService activityHistoryInfoService;


    /**
     * 校验L2,L3
     *
     * @param info
     * @return
     */
    @Override
    public R checkParams(CampaignCreateVo info) {
        /**
         * 1、判断类型为：L2/L3
         * 1.1 L2
         *      LV2的申请金额之和不能超过Campaign对应预算分类的所属资金池的授予金额
         *       1）MBF：不能超过所属国家的预算分类资金池
         * 		 2）MSF：不能超过HQ整体的预算分类资金池
         *
         *      LV2的申请金额之和不能超过Campaign对应国家的季度授予金额
         *
         * 1.2 L3
         *      LV3的活动时间不能超过所属Activity LV2的活动时间
         *      LV3的申请金额之和不能超过所属Activity LV2的申请金额（本币金额）
         *
         * 关于预算对Campaign的管控规则，跟业务多次讨论后结论如下：
         * 1、按季度授予总包管控Campaign申请金额，不需By资金池管控（业务上允许跨资金池腾挪，只要总包不超）
         * 2、按活动申请口径（提交Campaign日期）匹配截止当前季度授予额度，不是按Activity LV2日期匹配
         */

        log.info("ActivityInfoServiceImpl checkParams campaignCreateVo:{}", info.toString());
        // campaign 草稿状态跳过检验
        if ("0".equals(info.getClickType())) {
            return R.ok();
        }

        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());
        StringBuilder sb = new StringBuilder();

        // 查询预算分类信息
        Wrapper<BudgetTypeEntity> budgetTypeEntityWrapper = new EntityWrapper<>();
        budgetTypeEntityWrapper.eq("code", info.getBudgetType());
        budgetTypeEntityWrapper.last("limit 1");
        BudgetTypeEntity budgetTypeEntity = budgetTypeService.selectOne(budgetTypeEntityWrapper);
        if (budgetTypeEntity == null) {
            return R.error("budgetTypeEntity info is blank!");
        }

        List<ActivityHistoryInfoVo> infos = info.getActivityInfos();
        // 取所有币种的最新汇率
        List<ExchangeRateInfoVo> rates = exchangeService.getLastestRateList();
        // l2 l3 重要入参数检查
        for (ActivityHistoryInfoVo a2 : infos) {
            if (org.apache.commons.lang3.StringUtils.isAnyEmpty(a2.getCurrency(), a2.getSource(), a2.getLv1Subject(), a2.getLv2Subject())
                    || null == a2.getAmount()) {
                return R.error("Activity Lv2 param is blank!");
            }
            BigDecimal latestRateVal = getLatestRate(a2.getCurrency(), rates);
            BigDecimal initRate = a2.getInitRate() == null ? latestRateVal : a2.getInitRate();
            BigDecimal latestRate = a2.getLatestRate() == null ? latestRateVal : a2.getLatestRate();
            a2.setInitRate(initRate);
            a2.setLatestRate(latestRate);

            if (CollectionUtils.isNotEmpty(a2.getActivityL3s())) {
                for (ActivityHistoryInfoVo a3 : a2.getActivityL3s()) {
                    if (org.apache.commons.lang3.StringUtils.isAnyEmpty(a3.getActivityExecutor(), a3.getLv3Subject(), a3.getDescription())
                            || null == a3.getFinanceInfo() || null == a3.getFinanceInfo().getCurrency() || null == a3.getFinanceInfo().getSource()
                            || null == a3.getFinanceInfo().getApplyAmount() || null == a3.getFinanceInfo().getApplyAmountUsd()) {
                        return R.error("Activity Lv3 param is blank!");
                    }
                    BigDecimal latestRateL3Val = getLatestRate(a3.getFinanceInfo().getCurrency(), rates);
                    BigDecimal initRateL3 = a3.getInitRate() == null ? latestRateL3Val : a3.getInitRate();
                    BigDecimal latestRateL3 = a3.getLatestRate() == null ? latestRateL3Val : a3.getLatestRate();
                    a3.setInitRate(initRateL3);
                    a3.setLatestRate(latestRateL3);
                }
            }
        }

        // 查询卷积金额
        List<String> ids = info.getActivityInfos().stream().filter(f -> Strings.isNotBlank(f.getActivityId())).map(map -> map.getActivityId()).collect(Collectors.toList());
        BigDecimal sumAmount = getSumAmount(info.getYear(), AreaLevelEnum.REPRESENT_OFFICE.getLevel(), info.getRepresentative(), budgetTypeEntity.getSource(), ids);
        log.info("checkParams sumAmount = {}", sumAmount);

        // 判断费用来源
        if (MyConstant.MSF.equals(budgetTypeEntity.getSource())) {
            // 查询所有MSF活动，判断是否超过HQ当年金额
            Map<String, List<ActivityHistoryInfoVo>> act2Msfs = infos.stream().filter(f -> f.getSource().equals(MyConstant.MSF)).collect(Collectors.groupingBy(ActivityHistoryInfoVo::getCurrency));

            if (act2Msfs.size() > 0) {
                Wrapper<CountryMarketingBudgetEntity> mbeMsf = new EntityWrapper<>();
                mbeMsf.eq("year", info.getYear());
                mbeMsf.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
                mbeMsf.eq("level", AreaLevelEnum.WORLDWIDE.getLevel());
                mbeMsf.eq("source", MyConstant.MSF);
//                CountryMarketingBudgetEntity marketingBudgetEntity = marketingBudgetService.selectOne(mbeMsf);
                List<CountryMarketingBudgetEntity> countryMarketingBudgetEntities = marketingBudgetService.selectList(mbeMsf);
                if (countryMarketingBudgetEntities.size() == 0) {
                    return R.error(isChinese ? TipEnum.HQBudgetValid.getStatusNameCn() : TipEnum.HQBudgetValid.getStatusNameEn());
                }
                BigDecimal hqBd = countryMarketingBudgetEntities.stream().map(CountryMarketingBudgetEntity::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                // 根据分组后的币种查询汇率计算总额，再对比总包金额
                BigDecimal act2MsfTotal = BigDecimal.ZERO;
                for (String currency : act2Msfs.keySet()) {
//                    BigDecimal queryExchange = getBigDecimal(currency);
                    List<ActivityHistoryInfoVo> acs = act2Msfs.get(currency);
                    for (ActivityHistoryInfoVo acInfo: acs) {
                        // 入参需要有最新汇率和回冲金额
                        act2MsfTotal = act2MsfTotal.add(MathUtils.subtract(acInfo.getAmount().multiply(acInfo.getLatestRate()), acInfo.getRecoveryAmountUsd()));
                    }
                }
                // 变更幅度控制在上一次lv2卷积和的50%范围内
                String checkRes = checkChangeReviewParam(info, act2MsfTotal);
                if (StringUtils.isNotEmpty(checkRes)) {
                    return R.error(checkRes);
                }
                act2MsfTotal = act2MsfTotal.add(sumAmount);
                act2MsfTotal = act2MsfTotal.setScale(CommonConstant.SCALE, BigDecimal.ROUND_HALF_UP);
                log.info("MSF act2MsfTotal= {}, hqBd = {}", act2MsfTotal, hqBd);
                if (act2MsfTotal.compareTo(hqBd) > 0) {
                    return R.error(isChinese ? TipEnum.Act2MSFAmountValid.getStatusNameCn() : TipEnum.Act2MSFAmountValid.getStatusNameEn());
                }
            }
        } else {
            // 根据代表处code和campaign提交日期查询某一年季度总包
            Wrapper<CountryQuarterBudgetEntity> cqbe = new EntityWrapper<>();
            cqbe.in("area_code", info.getRepresentative());
            cqbe.eq("year", info.getYear());
            cqbe.eq("status", MyConstant.DEL_STAUTS_NORMAL);
            List<CountryQuarterBudgetEntity> cqbeList = countryQuarterBudgetService.selectList(cqbe);
            BigDecimal aqbeAmount = cqbeList.stream().map(p -> p.getSumAmount()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            Map<String, List<ActivityHistoryInfoVo>> act2Mbfs = infos.stream().filter(f -> f.getSource().equals(MyConstant.MBF)).collect(Collectors.groupingBy(ActivityHistoryInfoVo::getCurrency));

            BigDecimal act2MbfToatl = BigDecimal.ZERO; // l2 mbf实际使用金额汇总
            for (String currency : act2Mbfs.keySet()) {
//                BigDecimal queryExchange = getBigDecimal(currency);
                List<ActivityHistoryInfoVo> acs = act2Mbfs.get(currency);
                for (ActivityHistoryInfoVo acInfo: acs) {
                    act2MbfToatl = act2MbfToatl.add(MathUtils.subtract(acInfo.getAmount().multiply(acInfo.getLatestRate()), acInfo.getRecoveryAmountUsd()));
                }
            }
            log.info("checkParams act2MbfToatlis = {}", act2MbfToatl);
            // 变更幅度控制在上一次lv2卷积和的50%范围内
            String checkRes = checkChangeReviewParam(info, act2MbfToatl);
            if (StringUtils.isNotEmpty(checkRes)) {
                return R.error(checkRes);
            }

            act2MbfToatl= act2MbfToatl.add(sumAmount); // 本次提交的L2金额 + 已有的
            act2MbfToatl = act2MbfToatl.setScale(CommonConstant.SCALE, BigDecimal.ROUND_HALF_UP);
            log.info("MBF act2MsfTotal= {}, hqBd = {}", act2MbfToatl, aqbeAmount);
            // -1-小于,0-等于,1-大于
            if (act2MbfToatl.compareTo(aqbeAmount) == 1) {
                return R.error(isChinese ? TipEnum.Act2MBFAmountValid.getStatusNameCn() : TipEnum.Act2MBFAmountValid.getStatusNameEn());
            }
        }

        // ActivityL3约束
        lxk:
        for (ActivityHistoryInfoVo a : infos) {
            if (a.getActivityL3s() != null) {
                BigDecimal bd = BigDecimal.ZERO;
                BigDecimal lv2Bd = MathUtils.multiply(a.getLatestRate(), a.getAmount());
//                BigDecimal lv2Bd = getBigDecimal(a.getCurrency()).multiply(a.getAmount());
                for (ActivityHistoryInfoVo a3 : a.getActivityL3s()) {
                    // LV3活动时间不能超过所属Activity LV2的活动时间
                    if (a3.getStartDate().isBefore(a.getStartDate()) || a3.getEndDate().isAfter(a.getEndDate())) {
                        sb.append(I18nUtil.getMessage("ActL3DateValid"));
                        // break lxk 是直接跳出多层for循环，不再继续执行for循环到代码了
                        break lxk;
                    }
                    FinanceInfo financeInfo = a3.getFinanceInfo();
                    // 汇总LV3实际使用金额
                    if (null != a3.getId()) {
                        // L3存在就累加实际使用金额USD
                        ActivityHistoryInfoVo dbA3Info = activityHistoryInfoService.selectById(a3.getId());
                        BigDecimal lv3ApplyAmountUsd = MathUtils.multiply(a3.getFinanceInfo().getApplyAmount(), a3.getLatestRate());
//                        BigDecimal lv3ApplyAmountUsd = getBigDecimal(financeInfo.getCurrency()).multiply(financeInfo.getApplyAmount()); // 此次填写的L3金额USD

                        // 考虑到草稿也会有id， 所以草稿状态下的实际使用金额
                        BigDecimal actuallyUsedAmountUsd = (null == dbA3Info || null == dbA3Info.getRecoveryAmountUsd()) ? lv3ApplyAmountUsd : lv3ApplyAmountUsd.subtract(dbA3Info.getRecoveryAmountUsd());
                        a3.setActuallyUsedAmountUsd(actuallyUsedAmountUsd);
                        bd = bd.add(actuallyUsedAmountUsd);
                    } else {
                        // 设置值， 方便保存L3时保存值
                        BigDecimal lv3ApplyAmountUsd = MathUtils.multiply(a3.getFinanceInfo().getApplyAmount(), a3.getLatestRate());
                        a3.setActuallyUsedAmountUsd(lv3ApplyAmountUsd);
                        // 当前L2下的L3累加申请金额USD
                        bd = bd.add(a3.getFinanceInfo().getApplyAmountUsd());
                    }
                }
                // LV3的申请金额（回冲后就是实际使用金额了）之和不能超过所属Activity LV2的申请金额（本币金额）
                info.setLv2AmountUsdTotal(MathUtils.round(lv2Bd,2));
                if (MathUtils.round(bd, 2).compareTo(MathUtils.round(lv2Bd,2)) > 0) {
                    String desc = a.getDescription().length() > 20 ? a.getDescription().substring(0,20)+"..." : a.getDescription();
                    String tip = isChinese ? TipEnum.ActAmountValid.getStatusNameCn() : TipEnum.ActAmountValid.getStatusNameEn();
                    sb.append(desc).append(":").append(tip);
                    break;
                }
            }
        }
        return sb.length() > 0 ? R.error(sb.toString()) : R.ok();
    }

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

    /**
     * 变更幅度控制在上一次lv2卷积和的50%范围内
     *
     * @param campaignCreateVo
     * @param changeReiviewlv2SumAmout
     * @return
     */
    private String checkChangeReviewParam(CampaignCreateVo campaignCreateVo, BigDecimal changeReiviewlv2SumAmout) {
        // 获取上一有效版本的lv2卷积和
        Wrapper<CampaignHistoryInfoVo> campWrapper = new EntityWrapper<>();
        campWrapper.eq("campaign_id", campaignCreateVo.getCampaignId());
        campWrapper.ne("id", campaignCreateVo.getId());
        campWrapper.orderBy("id", false);
        List<CampaignHistoryInfoVo> allCampaignHisVos = campaignHistoryInfoService.selectList(campWrapper);
        if (CollectionUtils.isEmpty(allCampaignHisVos) ||
                (allCampaignHisVos.size() == 1 && StringUtils.equals(allCampaignHisVos.get(0).getStatus(),
                        CampaignStatusEnum.DRAFT.getStatusCode())) ||
                (allCampaignHisVos.size() == 1 && StringUtils.equals(allCampaignHisVos.get(0).getStatus(),
                        CampaignStatusEnum.APPROVAL_FAILED.getStatusCode()))) {
            // 非变更场景 or 草稿提交场景下，不校验
            return null;
        }

        // campaign其它版本不允许有在途版本（存在审批中的版本）
        List<String> allStatus =
                allCampaignHisVos.stream().map(CampaignHistoryInfoVo::getStatus).distinct().collect(Collectors.toList());
        List<String> processIngStatus = Arrays.asList(CampaignStatusEnum.DRAFT.getStatusCode(),
                CampaignStatusEnum.WAITING_REVIEW.getStatusCode(),
                CampaignStatusEnum.WAITING_APPROVAL.getStatusCode(),
                CampaignStatusEnum.APPROVAL_FAILED.getStatusCode(), CampaignStatusEnum.CHANGE_FAILED.getStatusCode());
        for (String str : allStatus) {
            if (processIngStatus.contains(str)) {
                return I18nUtil.getMessage("CampaignHasIntransit");
            }
        }

        // 2. 不能超过上一版本LV2卷积之和的50%
        List<CampaignHistoryInfoVo> previousCampaignHisVos =
                allCampaignHisVos.stream().filter(item -> StringUtils.equals(item.getIsValidVersion(),
                        CommonConstant.VALID_VERISON_EN) && StringUtils.equals(item.getStatus(),
                        CampaignStatusEnum.APPROVAL_SUCCESS.getStatusCode())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(previousCampaignHisVos)) {
            Wrapper<ActivityHistoryInfoVo> lvWrapper = new EntityWrapper<>();
            lvWrapper.eq("parent_id", campaignCreateVo.getCampaignId());
            lvWrapper.eq("ref_id", previousCampaignHisVos.get(0).getId());
            lvWrapper.eq("level", CommonConstant.LV2_LEVEL);
            lvWrapper.eq("del_status", DeleteStatusEnum.NORMAL.getStatusCode());
            List<ActivityHistoryInfoVo> previousLv2Infos = activityHistoryInfoService.selectList(lvWrapper);
            BigDecimal previousLv2SumAmout = BigDecimal.ZERO;
            for (ActivityHistoryInfoVo previousLv2Vo : previousLv2Infos) {
                previousLv2SumAmout =
                        previousLv2SumAmout.add(previousLv2Vo.getAmount().multiply(previousLv2Vo.getLatestRate()));
            }

            SysDictEntity sysDict = sysDictService.getSysDist("lv2AmoutChangeRange", "1");
            BigDecimal lv2AmoutChangeMax =
                    previousLv2SumAmout.add(previousLv2SumAmout.multiply(new BigDecimal(sysDict.getValue())));
            if (changeReiviewlv2SumAmout.compareTo(lv2AmoutChangeMax) > 0) {
                return I18nUtil.getMessage("Lv2AmountChangeRange");
            }
        }

        return null;
    }

    // 获取卷积金额
    private BigDecimal getSumAmount(String year, String level, String areaCode, String source, List<String> ids) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("year", year);
        map.put("source", source);
        map.put("ids", ids);
        // 全球不用设值
        if (AreaLevelEnum.REGION.getLevel().equals(level)) {
            map.put("areaCode", areaCode);
        } else if (AreaLevelEnum.REPRESENT_OFFICE.getLevel().equals(level)) {
            map.put("representative", areaCode);
        }
        // 查询国家地区已收益金额
        List<AmountVO> amountVOS = budgetDao.queryAmountRateV2(map);
        BigDecimal benefitAmount = BigDecimal.ZERO;
        for (AmountVO amountVO : amountVOS) {
            if (CampaignStatusEnum.CLOSED.getStatusCode().equals(amountVO.getCampaignStatus())) {
                // // 此时计算国家地区已收益金额的时候， 只计算实际使用金额（际使用金额 = 申请金额 - 回冲金额）
                benefitAmount = benefitAmount.add(MathUtils.subtract(amountVO.getAmount().multiply(amountVO.getSaveLatestRate()), amountVO.getRecoveryAmountUsd()));
            } else {
                benefitAmount = benefitAmount.add(amountVO.getAmount().multiply(amountVO.getSaveLatestRate()));
            }
        }
        return benefitAmount;
    }

    public BigDecimal getBigDecimal(String currency){
        Map<String, Object> param = new HashMap<>();
        param.put("fromCurrency", currency);
        param.put("rateDate", getYearAndMonth());
        BigDecimal queryExchange = exchangeService.queryExchange(param);
        return queryExchange == null ? BigDecimal.ZERO : queryExchange;
    }

    /**
     * 活动Id自增
     *
     * @return
     */
    public StringBuilder increaseActId(StringBuilder sb) {
        String num = sb.substring(7);// sb.substring(2)去掉前两个字符
        int num1 = Integer.parseInt(num);
        num1++;
        String str = String.format("%03d", num1);// 如果小于6位左边补0
        String ret = sb.substring(0, 7) + str;
        return new StringBuilder(ret);
    }

    public String getYearAndMonth() {
        LocalDate now = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        return sb.append(now.getYear()).append(now.getMonthValue() < 10 ? "0" + now.getMonthValue() : now.getMonthValue()).toString();
    }

    public List<Product> getBenefitProdByDict(){
        List<SysDictEntity> dicts = sysDictService.findByType("benefitProd");
        List<Product> prods = new ArrayList<>();
        dicts.forEach(d -> {
            Product p = new Product();
            p.setMatNo(d.getValue());
            p.setModel(d.getValue());
            prods.add(p);
        });
        return prods;
    }
}
