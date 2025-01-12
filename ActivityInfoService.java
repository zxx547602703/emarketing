package com.wiko.emarket.service.campaign;


import com.framework.common.utils.R;
import com.wiko.emarket.vo.CampaignCreateVo;
import com.wiko.psi.entity.Product;

import java.util.List;

public interface ActivityInfoService {
    R checkParams(CampaignCreateVo info);

    List<Product> getBenefitProdByDict();
}
