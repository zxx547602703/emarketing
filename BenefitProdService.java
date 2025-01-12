package com.wiko.emarket.service.campaign;

import com.baomidou.mybatisplus.service.IService;
import com.framework.common.utils.R;
import com.wiko.emarket.entity.BenefitProd;

import java.util.List;

/**
 * 收益产品管理
 */
public interface BenefitProdService extends IService<BenefitProd> {
    List<BenefitProd> getProdByActId(String actId, Integer processId);

    R save(BenefitProd benefitProd);
}
