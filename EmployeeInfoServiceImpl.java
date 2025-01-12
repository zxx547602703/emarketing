package com.wiko.emarket.service.campaign.impl;

import com.framework.modules.sys.shiro.ShiroUtils;
import com.wiko.emarket.dao.EmployeeInfoDao;
import com.wiko.emarket.service.campaign.EmployeeInfoService;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.EmployeeInfoVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @ClassName EmployeeInfoServiceImpl
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 15:57
 * @Version 1.0
 **/
@Service
public class EmployeeInfoServiceImpl implements EmployeeInfoService {

    @Autowired
    private EmployeeInfoDao employeeInfoDao;

    /**
     * 获取当前用户部门
     *
     * @return String
     */
    @Override
    public String queryDeptName() {
        HashMap<String, Object> map = new HashMap<>(2);
        map.put("userId", ShiroUtils.getUserId());
        map.put("language", RequestUtil.getLang());
        return employeeInfoDao.queryDeptName(map);
    }

    @Override
    public EmployeeInfoVO queryEmpNo(String empNo) {
        HashMap<String, Object> map = new HashMap<>(2);
        if(StringUtils.isEmpty(empNo)){
            return null;
        }
        map.put("empNo",empNo);
        map.put("lang", RequestUtil.getLang());
        return employeeInfoDao.queryEmpNo(map);
    }

}
