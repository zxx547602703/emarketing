package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.framework.common.utils.Dates;
import com.framework.common.utils.PageUtil;
import com.framework.common.utils.PageUtils;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.constant.*;
import com.wiko.emarket.dao.ActivityHistoryInfoDao;
import com.wiko.emarket.dao.ActivityTypeDao;
import com.wiko.emarket.dao.AreaDao;
import com.wiko.emarket.dao.CompanyDao;
import com.wiko.emarket.entity.*;
import com.wiko.emarket.service.acceptance.*;
import com.wiko.emarket.service.campaign.CampaignNewReportService;
import com.wiko.emarket.service.campaign.ExchangeService;
import com.wiko.emarket.service.emarketprocess.impl.CampaignCommonService;
import com.wiko.emarket.util.MathUtils;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.CompanyVO;
import com.wiko.emarket.vo.campaignReport.CampaignExcuteVo;
import com.wiko.emarket.vo.campaignReport.ExportCampaignExecuteDetailEnVo;
import com.wiko.psi.util.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
@Slf4j
public class CampaignNewReportServiceImpl implements CampaignNewReportService {

    @Autowired
    private ActivityHistoryInfoDao activityHistoryInfoDao;

    @Autowired
    private PoService poService;

    @Autowired
    private PoLineService poLineService;

    @Autowired
    private AcceptanceService acceptanceService;

    @Autowired
    private ReceiptDetailInfoService receiptDetailInfoService;

    @Autowired
    private ReceiptInfoService receiptInfoService;

    @Autowired
    private PaymentVoucherInfoService paymentVoucherInfoService;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private CampaignCommonService campaignCommonService;

    @Autowired
    private AreaDao areaDao;

    @Autowired
    private ActivityTypeDao activityTypeDao;

    @Autowired
    private CompanyDao companyDao;


    @Override
    public PageUtils getCampaignExecuteList(Map<String, Object> params, Long userId) {
        int page = Integer.parseInt(params.get("currentPage").toString());
        int size = Integer.parseInt(params.get("pagesize").toString());
        boolean isChinese = CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());

        List<CampaignExcuteVo> campaignExcuteVos = getCampaignExcuteVos(params, isChinese);
        // 分页返回
        return new PageUtils(PageUtil.limit(campaignExcuteVos, size, page), campaignExcuteVos.size(), size, page);
    }

    @Override
    public void exportCampaignExecute(Map<String, Object> params, Long userId, HttpServletResponse response) throws IOException {
        boolean isChinese = CommonConstant.ZH_CN_LANGUAGE.equals(params.get("language"));
        // 查询数据
        long time1 = System.currentTimeMillis();
        List<CampaignExcuteVo> allDatas = getCampaignExcuteVos(params, isChinese);
        long time2 = System.currentTimeMillis();
        log.info("query campaign execute detail excel data, took time=====>{}", time2 - time1);

        // 导出数据
        String title = isChinese ? "Campaign明细执行清单" : "Campaign execution details";
        String fileName = title + Dates.getDateString();

        long time3 = System.currentTimeMillis();
        if(isChinese) {
            CommonsUtils.exportExcel(title, fileName, allDatas, CampaignExcuteVo.class, response);
        } else {
            CommonsUtils.exportExcel(title, fileName, allDatas, ExportCampaignExecuteDetailEnVo.class, response);
        }
        //调用接口导出表格
        log.info("export campaign execute detail excel data end, took time =====>{}", (System.currentTimeMillis() - time3));
    }

    /**
     * 查询campaign报表-执行明细:收益产品名称*PO笛卡尔积维度查询
     * 
     * @param params
     * @param isChinese
     * @return
     */
    private List<CampaignExcuteVo> getCampaignExcuteVos (Map<String, Object> params, boolean isChinese) {
        // 1 收益产品名称*PO 笛卡尔积维度查询
        List<CampaignExcuteVo> campaignExcuteVos = activityHistoryInfoDao.queryCampaignReportPart(params);

        // 2 以PO为维度查询，仅查询最新
        List<String> prIds = campaignExcuteVos.stream().map(CampaignExcuteVo::getPrId).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
        Wrapper<PoInfo> poWrapper = new EntityWrapper<>();
        poWrapper.in("pr_id", prIds);
        poWrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
        poWrapper.orderBy("created_time", false);
        List<PoInfo> poInfos = poService.selectList(poWrapper);

        // 2.1 查询po_line_info
        Wrapper<PoLineInfo> polineWrapper = new EntityWrapper<>();
        polineWrapper.in("po_id", poInfos.stream().map(PoInfo::getPoId).distinct().collect(Collectors.toList()));
        polineWrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
        List<PoLineInfo> polineInfos = poLineService.selectList(polineWrapper);

        // 2.2 查询验收单， 仅查询完成评审且有效的验收单
        Wrapper<AcceptanceFormInfo> acWrapper = new EntityWrapper<>();
        acWrapper.in("po_id", poInfos.stream().map(PoInfo::getPoId).distinct().collect(Collectors.toList()));
        acWrapper.eq("status", AcceptanceFormStatusEnum.APPROVAL_SUCCESS.getStatusCode());
        acWrapper.eq("delete_status", DeleteStatusEnum.NORMAL.getStatusCode());
        acWrapper.orderBy("created_time", true);
        List<AcceptanceFormInfo> acceptanceFormInfos = acceptanceService.selectList(acWrapper);

        // 2.3 查询发票凭证, 仅查询有效的凭证
        Wrapper<ReceiptDetailInfo> redWrapper = new EntityWrapper<>();
        redWrapper.in("ref_po_id", poInfos.stream().map(PoInfo::getPoId).distinct().collect(Collectors.toList()));
        redWrapper.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
        redWrapper.orderBy("created_time", true);
        List<ReceiptDetailInfo> receiptDetailInfos = receiptDetailInfoService.selectList(redWrapper);

        Wrapper<ReceiptInfo> reWrapper = new EntityWrapper<>();
        reWrapper.in("id", receiptDetailInfos.stream().map(ReceiptDetailInfo::getRefId).distinct().collect(Collectors.toList()));
        reWrapper.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
        reWrapper.orderBy("created_time", true);
        List<ReceiptInfo> receiptInfos = receiptInfoService.selectList(reWrapper);

        // 2.4查询付款信息， 仅查询有效的付款信息
        Wrapper<PaymentVoucherInfo> payWrapper = new EntityWrapper<>();
        payWrapper.in("receipt_id", receiptInfos.stream().map(ReceiptInfo::getReceiptId).distinct().collect(Collectors.toList())).andNew();
        payWrapper.ne("reversal_voucher", YesNoEnum.YES.getCode()).or().isNull("reversal_voucher").andNew(); // 不等于Y的，即未冲销的, 包括null
        payWrapper.eq("status", DeleteStatusEnum.NORMAL.getStatusCode());
        payWrapper.orderBy("created_date", true);
        List<PaymentVoucherInfo> paymentVoucherInfos = paymentVoucherInfoService.selectList(payWrapper);

        // 2.5.1国家地区code映射
        List<AreaEntity> areaVoList = areaDao.selectList(null);
        Map<String, String> areaMap = new HashMap<>();
        if (isChinese) {
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameCn()));
        } else {
            areaVoList.stream().filter(areaVo -> areaVo.getCode() != null && !areaMap.containsKey(areaVo.getCode())).forEach(areaVo -> areaMap.put(areaVo.getCode(), areaVo.getNameEn()));
        }
        // 2.5.2业务科目类型映射
        List<ActivityType> activityTypeVoList = activityTypeDao.selectList(null);
        Map<String, String> activityTypeMap = new HashMap<>();
        if (isChinese) {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameCn()));
        } else {
            activityTypeVoList.stream().filter(activityType -> !activityTypeMap.containsKey(String.valueOf(activityType.getCode()))).forEach(activityType -> activityTypeMap.put(String.valueOf(activityType.getCode()), activityType.getNameEn()));
        }

        // 2.5.3 支付公司code映射, 不区分中英文
        List<CompanyVO> companyVoList = companyDao.queryComPanyList();
        Map<String, String> companyMap = new HashMap<>();
        companyVoList.stream().filter(item -> !companyMap.containsKey(String.valueOf(item.getCompanyCode()))).forEach(item -> companyMap.put(String.valueOf(item.getCompanyCode()), item.getCompanyName()));


        // 3 绑定PO相关数据
        campaignExcuteVos.forEach(vo -> {
            // 处理 campaign L2 L3基础数据
            String lv3ActivityExecutorUserCn = StringUtils.isEmpty(vo.getLv3ActivityExecutorUserEn()) && StringUtils.isEmpty(vo.getLv3ActivityExecutorUserCn()) ? null : (vo.getLv3ActivityExecutorUserEn() + " " + vo.getLv3ActivityExecutorUserCn()).trim();
            vo.setLv3ActivityExecutorUserCn(lv3ActivityExecutorUserCn);
            vo.setLv1Subject(activityTypeMap.get(vo.getLv1Subject()));
            vo.setLv2Subject(activityTypeMap.get(vo.getLv2Subject()));
            vo.setLv3Subject(activityTypeMap.get(vo.getLv3Subject()));
            vo.setBudgetBelongAreaName(areaMap.get(vo.getBudgetBelongAreaCode()));
            vo.setRepresentativeName(areaMap.get(vo.getRepresentativeCode()));
            vo.setPoPaymentCompanyName(companyMap.get(vo.getPoPaymentCompanyCode())); // 支付公司
            vo.setPrStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(vo.getPrStatusCode()) : PrPoStatusEnum.getNameEnByCode(vo.getPrStatusCode()));
            vo.setPoStatus(isChinese ? PrPoStatusEnum.getNameCnByCode(vo.getPoStatusCode()) : PrPoStatusEnum.getNameEnByCode(vo.getPoStatusCode()));
            if (StringUtils.isEmpty(vo.getPrId())) {
                // prId为空， 就没必须继续查询
                return;
            }
            // 当没有受益产品，则产品置空，按照100%分摊处理。
            vo.setProdRatio(null == vo.getProdRatio() ? new BigDecimal("100") : vo.getProdRatio());

            // 3.1 关联最新PO
            if (StringUtils.isEmpty(vo.getPoId())) {
                // poId为空， 就没必须继续查询
                return;
            }
            if (null == vo.getPoExchangeRate()) {
                //  如果po_info表中汇率字段值为空， 做兼容，重新查询
                vo.setPoExchangeRate(getExchangeRateUsd(vo.getPoCurrency(), Dates.dateToStr(vo.getPoCreatedTime(), Dates.FORMAT_DATETM)));
            }
            // PO金额
            List<PoLineInfo> filterPoLineInfos = polineInfos.stream().filter(item -> StringUtils.equals(item.getPoId(), vo.getPoId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(filterPoLineInfos)) {
                // 跳出本次循环
                return;
            }
            BigDecimal poAmount = filterPoLineInfos.stream().map(PoLineInfo::getSumAmount).reduce(BigDecimal::add).orElse(ZERO);
            // PO总金额 * 产品分摊比例  TODO此时还没验收 汇率那里来？
            vo.setPoAmount(computeRatio(poAmount, vo.getProdRatio()));
            computeRatio(MathUtils.multiplyNull(poAmount, vo.getPoExchangeRate()), vo.getProdRatio());
            vo.setPoAmountUsd(computeRatio(MathUtils.multiplyNull(poAmount, vo.getPoExchangeRate()), vo.getProdRatio()));
            vo.setPoTaxRate(null == filterPoLineInfos.get(0).getTaxRate() ? null : filterPoLineInfos.get(0).getTaxRate() + "%");
            vo.setPoIsincludedTaxName(null == filterPoLineInfos.get(0).getTaxRate() ? YesNoEnum.NO.getCode() : YesNoEnum.YES.getCode());


            // 3.2 关联正常的验收单
            List<AcceptanceFormInfo> filterAcceptanceFormInfos = acceptanceFormInfos.stream().filter(item -> StringUtils.equals(item.getPoId(), vo.getPoId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(filterAcceptanceFormInfos)) {
                vo.setAcceptanceIsOrNot(isChinese ? YesNoEnum.YES.getNameCn() : YesNoEnum.YES.getNameEn());
                vo.setAcceptanceDeliveryOrNot(vo.getAcceptanceIsOrNot());
                AcceptanceFormInfo dbAcceptanceFormInfo = filterAcceptanceFormInfos.get(0);
                vo.setAcceptanceDate(Date.from(dbAcceptanceFormInfo.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant()));
                BigDecimal acceptanceAmount = filterAcceptanceFormInfos.stream().map(AcceptanceFormInfo::getAcceptanceAmount).reduce(BigDecimal::add).orElse(ZERO);
                vo.setAcceptanceAmount(computeRatio(acceptanceAmount, vo.getProdRatio()));
                BigDecimal acceptanceAmountUsd = ZERO;
                for (AcceptanceFormInfo item : filterAcceptanceFormInfos) {
                    acceptanceAmountUsd = acceptanceAmountUsd.add(MathUtils.multiply(item.getAcceptanceAmount(), item.getExchangeRate()));
                }
                vo.setAcceptanceAmountUsd(computeRatio(acceptanceAmountUsd, vo.getProdRatio()));
                if (StringUtils.isNotEmpty(dbAcceptanceFormInfo.getFirstReviewer())) {
                    vo.setAcceptanceFirstReviewerUserId(Long.parseLong(dbAcceptanceFormInfo.getFirstReviewer()));
                    SysUserEntity sysUser = campaignCommonService.getUserByUserId(Long.parseLong(dbAcceptanceFormInfo.getFirstReviewer()));
                    if (null != sysUser) {
                        vo.setAcceptanceFirstReviewerUserCn(sysUser.getUserCard());
                    }
                }
            } else {
                vo.setAcceptanceIsOrNot(isChinese ?  YesNoEnum.NO.getNameCn() : YesNoEnum.YES.getNameEn());
                vo.setAcceptanceDeliveryOrNot(vo.getAcceptanceIsOrNot());
                return;
            }

            // 3.3 关联发票凭证 receipt_detail_info
            List<ReceiptDetailInfo> filterReceiptDetailInfos = receiptDetailInfos.stream().filter(item -> StringUtils.equals(item.getRefPoId(), vo.getPoId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(filterReceiptDetailInfos)) {
                vo.setReceiptIsInvoice(isChinese ? YesNoEnum.NO.getNameCn() : YesNoEnum.NO.getNameEn());
                return;
            }
            List<Integer> filterReceiptUniqueIds = filterReceiptDetailInfos.stream().map(ReceiptDetailInfo::getRefId).distinct().collect(Collectors.toList());
            List<ReceiptInfo> filterReceiptInfos= receiptInfos.stream().filter(item -> filterReceiptUniqueIds.contains(item.getId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(filterReceiptInfos)) {
                vo.setReceiptIsInvoice(isChinese ? YesNoEnum.YES.getNameCn() : YesNoEnum.YES.getNameEn());
                ReceiptInfo dbReceiptInfo = filterReceiptInfos.get(0);

                BigDecimal receiptSumAmount = filterReceiptInfos.stream().map(ReceiptInfo::getSumAmount).reduce(BigDecimal::add).orElse(ZERO);
                vo.setReceiptSumAmount(computeRatio(receiptSumAmount, vo.getProdRatio()));
                if (StringUtils.isNotEmpty(dbReceiptInfo.getCreatedTime())) {
                    vo.setReceiptDate(Dates.str2date(dbReceiptInfo.getCreatedTime(), Dates.PATTERN_DATETM));
                }
            }

            // 关联付款信息(receipt_detail_info==>receipt_info==>payment_voucher_info, 取未冲销且有效数据)
            List<String> filterReceiptIds = filterReceiptInfos.stream().map(ReceiptInfo::getReceiptId).collect(Collectors.toList());
            List<PaymentVoucherInfo> filterPaymentVoucherInfos = paymentVoucherInfos.stream().filter(item -> filterReceiptIds.contains(item.getReceiptId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(filterPaymentVoucherInfos)) {
                StringBuffer sbPayOrderNo = new StringBuffer();
                StringBuffer sbPayDate= new StringBuffer();
                BigDecimal paymentAmount = ZERO;
                BigDecimal paymentAmountUsd = ZERO;
                for (int i = 0; i < filterPaymentVoucherInfos.size(); i++) {
                    PaymentVoucherInfo payVo = filterPaymentVoucherInfos.get(i);
                    sbPayOrderNo.append(i + 1).append("、").append(payVo.getPaymentId()).append("\n");
                    if (null != payVo.getPaymentDate()) {
                        String payDateStr = Dates.dateToStr(Dates.str2date(payVo.getPaymentDate(), Dates.FORMAT_DATETM1), Dates.FORMAT_DATE);
                        sbPayDate.append(i + 1).append("、").append(payDateStr).append(" ").append(payVo.getCurrency()).append(" ").append(payVo.getPaymentAmount()).append("\n");
                    }
                    if (null != payVo.getPaymentAmount()) {
                        paymentAmount = paymentAmount.add(payVo.getPaymentAmount());
                        if (null != payVo.getExchangeRateUsd()) {
                            // 优先使用数据库已存储的汇率， 没有存储汇率就再实时获取一次
                            paymentAmountUsd = paymentAmountUsd.add(MathUtils.multiply(payVo.getPaymentAmount(), payVo.getExchangeRateUsd()));

                        } else {
                            if (null != payVo.getCurrency()) {
                                // 求付款时对应的汇率, 没有对应的汇率就取最新汇率
                                Map<String, Object> param = new HashMap<>();
                                param.put("fromCurrency", payVo.getCurrency());
                                param.put("rateDate", Dates.dateToStr(Dates.str2date(payVo.getPaymentDate(), Dates.FORMAT_DATETM1), Dates.FORMAT_DATETM2));
                                BigDecimal rate = exchangeService.queryExchange(param);
                                paymentAmountUsd = paymentAmountUsd.add(MathUtils.multiply(payVo.getPaymentAmount(), rate));
                            }
                        }

                    }
                }
                vo.setPaymentIsOrNot(isChinese ? CommonConstant.PaymentEnum.YES.getNameCn() : CommonConstant.PaymentEnum.YES.getNameEn());
                vo.setPaymentOrderNo(sbPayOrderNo.toString());
                vo.setPaymentDate(sbPayDate.toString());
                vo.setPaymentAmount(computeRatio(paymentAmount, vo.getProdRatio()));
                vo.setPaymentAmountUsd(computeRatio(paymentAmountUsd, vo.getProdRatio()));
                if (null != vo.getPaymentAmountUsd() && null != vo.getAcceptanceAmountUsd()) {
                    // 用本币种求比列，用USD会因为时间不同，导致汇率不同，会出现虽然100%付款，但USD参与计算时会存在误差
                    BigDecimal paymentRatio = MathUtils.devide(vo.getPaymentAmount().multiply(new BigDecimal("100")), vo.getAcceptanceAmount());
                    vo.setPaymentRatio(paymentRatio.toString() + "%");
                }
            } else {
                vo.setPaymentIsOrNot(isChinese ? CommonConstant.PaymentEnum.NO.getNameCn() : CommonConstant.PaymentEnum.NO.getNameEn());
            }
        });

        // 根据sql以外的条件进一步过滤
        if (!Objects.isNull(params.get("poCurrency")) && StringUtils.isNotEmpty((String) params.get("poCurrency"))) {
            // PO币种， 精确
            String poCurrency = (String) params.get("poCurrency");
            campaignExcuteVos = campaignExcuteVos.stream().filter(vo -> StringUtils.equalsIgnoreCase(vo.getPoCurrency(), poCurrency)).collect(Collectors.toList());
        }
        if (!Objects.isNull(params.get("poSupplierName")) && StringUtils.isNotEmpty((String) params.get("poSupplierName"))) {
            // 供应商模糊搜索
            String poSupplierName = (String) params.get("poSupplierName");
            campaignExcuteVos = campaignExcuteVos.stream().filter(vo -> StringUtils.contains(vo.getPoSupplierName(), poSupplierName)).collect(Collectors.toList());
        }

        return campaignExcuteVos;
    }

    /**
     * 金额*产品1分摊比例
     *
     * @param amount 1000
     * @param ratio  13
     * @return 1000 * 13 /100
     */
    private BigDecimal computeRatio(BigDecimal amount, BigDecimal ratio) {
        if (null == amount ||  null == ratio) {
            return null;
        }

        return amount.multiply(ratio).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取汇率
     *
     * @param currency
     * @param rateDate
     * @return
     */
    private BigDecimal getExchangeRateUsd(String currency, String rateDate) {
        // 求对应的汇率, 没找到就取最新汇率
        Map<String, Object> param = new HashMap<>();
        param.put("fromCurrency", currency);
        param.put("rateDate", rateDate);
        return exchangeService.queryExchange(param);
    }
}
