package com.wiko.emarket.service.campaign.impl;

import com.wiko.emarket.dao.CompanyDao;
import com.wiko.emarket.service.campaign.CompanyService;
import com.wiko.emarket.vo.CompanyVO;
import com.wiko.emarket.vo.CostCenterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @ClassName CompanyServiceImpl
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 17:15
 * @Version 1.0
 **/
@Service
public class CompanyServiceImpl implements CompanyService {
    @Autowired
    private CompanyDao companyDao;
    @Override
    public List<CompanyVO> queryComPanyList() {
        return companyDao.queryComPanyList();
    }

    @Override
    public List<CostCenterVO> queryCostCenter(Map<String, Object> params) {
        return companyDao.queryCostCenter(params);
    }
}
