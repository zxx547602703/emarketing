package com.wiko.emarket.service.campaign;

import com.wiko.emarket.vo.EmployeeInfoVO;

/**
 * @ClassName EmployeeInfoService
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 15:50
 * @Version 1.0
 **/
public interface EmployeeInfoService {
    /**
     * 获取当前用户部门
     *
     * @return String
     */
   String queryDeptName();

    /**
     * 查询工号是否存在
     *
     * @param empNo
     * @return Integer
     */
    EmployeeInfoVO queryEmpNo(String empNo);

}
