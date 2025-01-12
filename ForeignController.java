package com.wiko.emarket.controller.foreign;

import com.framework.common.utils.R;
import com.framework.modules.sys.dao.SysUserDao;
import com.framework.modules.sys.entity.SysUserEntity;
import com.wiko.emarket.dao.EmployeeInfoDao;
import com.wiko.emarket.service.acceptance.PoService;
import com.wiko.emarket.vo.EmployeeInfoVO;
import com.wiko.emarket.vo.RequestDeptVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 外部系统调用emarketing的接口， 对外接口， 员工账号信息、 保存PO
 *
 * @ClassName TestController
 * @Description TODO
 * @Author yanhui.zhao
 * @Date 2022/6/24 9:45
 * @Version 1.0
 **/
@RestController
@RequestMapping("/foreign")
@Slf4j
public class ForeignController {
    @Autowired
    private EmployeeInfoDao employeeInfoDao;

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private PoService poService;

    private static final String EMPLOYEE_STATUS_UNABLE = "离职人员";

    @RequestMapping("/getEmployeeInfo")
    @Transactional(rollbackFor = Exception.class)
    public R getEmployeeInfo(@RequestBody (required =false)Map<String, List<EmployeeInfoVO>> params) {
        log.info("ForeignController getEmployeeInfo info:" + params);
        if (null == params || CollectionUtils.isEmpty(params.get("getEmployeeInfo"))) {
            log.debug("ForeignController getEmployeeInfo info is empty");
            return R.ok();
        }

        List<EmployeeInfoVO> employeeInfoList = params.get("getEmployeeInfo");
        try {
            // 1 保存员工账号信息
            employeeInfoDao.saveEmployeeInfo(employeeInfoList);
            log.info("ForeignController saveEmployeeInfo success");

            // 2 将离职员工设为禁用状态(检查本地的已离职的数据)
            updateSysUserStatusByEmpNo();
            log.info("ForeignController getEmployeeInfo end");
        } catch (Exception e) {
            log.error("ForeignController getEmployeeInfo save error:" + e);
            return R.error("保存HR系统人员信息失败,请联系系统管理员");
        }
        return R.ok();
    }

    /**
     * 更新员工信息状态， 离职员工状态设为禁用
     *
     */
    private void updateSysUserStatusByEmpNo() {
        // 将离职员工设为禁用状态
        try {
            List<EmployeeInfoVO> unableEmpList = employeeInfoDao.queryUnableEmpList();
            List<SysUserEntity> updateList = new ArrayList<>();
            for (EmployeeInfoVO vo: unableEmpList){
                if (EMPLOYEE_STATUS_UNABLE.equals(vo.getStatus())) {
                    SysUserEntity sysUserEntity = new SysUserEntity();
                    sysUserEntity.setEmpNo(vo.getEmpNo());
                    // 1正常， 0禁用
                    sysUserEntity.setStatus(0);
                    updateList.add(sysUserEntity);
                }
            }
            log.info("updateSysUserStatusByEmpNo empNo = {}", updateList.stream().map(SysUserEntity::getEmpNo).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(updateList)) {
                sysUserDao.updateSysUserStatusByEmpNo(updateList);
            }
        } catch (Exception e) {
            log.error("updateSysUserStatusByEmpNo exception", e);
        }
    }

    @RequestMapping("/getDeptInfo")
    @Transactional(rollbackFor = Exception.class)
    public R getDeptInfo(@RequestBody Map<String, List<RequestDeptVO>> params) {
        log.error("ForeignController getDeptInfo info:" + params);
        List<RequestDeptVO> deptInfoList = params.get("getDeptInfo");

        HashMap<String, String> deptIdNoMap = new HashMap<String, String>();
        List<RequestDeptVO> deptInfoVOS = employeeInfoDao.queryEmployeeInfoList();
        for (RequestDeptVO deptInfo : deptInfoVOS) {

            deptIdNoMap.put(deptInfo.getDeptNo(), deptInfo.getDeptId());
        }
        for (RequestDeptVO vo : deptInfoList) {
                deptIdNoMap.put(vo.getDeptNo(), vo.getDeptId());

        }
        for (RequestDeptVO vo : deptInfoList) {
            if (deptIdNoMap.containsKey(vo.getParentId())) {
                vo.setParentId(deptIdNoMap.get(vo.getParentId()));
            } else if ("ROOT".equals(vo.getParentId())) {
                vo.setParentId("0");
            }
        }
        log.warn("ForeignController getDeptInfo type conversion success:" + deptInfoList);
        try {
            employeeInfoDao.saveDeptInfo(deptInfoList);

        } catch (Exception e) {
            log.error("ForeignController getDeptInfo save error:" + e);
            return R.error();
        }

        return R.ok();
    }

    /**
     * 保存PO信息
     *
     * @param params
     * @return
     */
    @RequestMapping("/save")
    public R save(@RequestBody Map<String, Object> params) {
        return poService.save(params);
    }
}
