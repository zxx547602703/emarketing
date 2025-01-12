package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.wiko.emarket.service.campaign.EmployeeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @ClassName CommonController
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 10:33
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/employee")
public class EmployeeInfoController {
    @Autowired
    private EmployeeInfoService employeeInfo;
    /**
     * 获取当前用户部门
     *
     * @return String
     */
    @RequestMapping("/getUserDeptName")
    public R getUserDeptName(){

        return R.ok().put("data",employeeInfo.queryDeptName());
    }
    /**
     * 获取当前用户部门
     *
     * @return String
     */
    @RequestMapping("/queryEmpNo")
    public R queryEmpNo(@RequestParam Map<String,String> map){
        return R.ok().put("data",employeeInfo.queryEmpNo(map.get("empNo")));
    }


}
