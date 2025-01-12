package com.wiko.emarket.dao;

import com.wiko.emarket.vo.EmployeeInfoVO;
import com.wiko.emarket.vo.RequestDeptVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @ClassName EmployeeInfoDao
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/7/4 10:50
 * @Version 1.0
 **/
@Mapper
public interface EmployeeInfoDao {
    /**
     * 保存工号信息
     *
     * @param employeeInfoList
     */
    void saveEmployeeInfo(List<EmployeeInfoVO> employeeInfoList);
    /**
     * 保存工号信息
     *
     * @param deptInfoList
     */
    void saveDeptInfo(List<RequestDeptVO> deptInfoList);
    /**
     * 获取当前用户部门
     *
     * @param params
     * @return String
     */
    String queryDeptName(Map<String,Object> params);
    /**
     * 获取当前用户部门
     *
     * @param params
     * @return Integer
     */
    EmployeeInfoVO queryEmpNo(Map<String,Object> params);
    /**
     * 获取所有的部门id和编号
     *
     * @return Integer
     */
    List<RequestDeptVO> queryEmployeeInfoList();

    /**
     * 查询已离职员工
     *
     * @return
     */
    List<EmployeeInfoVO> queryUnableEmpList();
}
