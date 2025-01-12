package com.wiko.emarket.service.campaign.impl;

import com.framework.common.exception.RRException;
import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysRoleEntity;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysRoleService;
import com.framework.modules.sys.service.SysUserRoleService;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.activiti.constant.ProcessTaskStatusEnum;
import com.wiko.activiti.constant.ProcessTypeEnum;
import com.wiko.activiti.constant.ProcessVisiableEnum;
import com.wiko.activiti.dao.ProcessTaskInfoDao;
import com.wiko.activiti.service.task.BpmTaskService;
import com.wiko.activiti.vo.ProcessTaskInfoVo;
import com.wiko.emarket.constant.AcceptanceFormStatusEnum;
import com.wiko.emarket.constant.CampaignStatusEnum;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.constant.RoleNameEnum;
import com.wiko.emarket.dao.ApprovalDao;
import com.wiko.emarket.service.acceptance.AcceptanceFormInfoService;
import com.wiko.emarket.service.campaign.ApprovalService;
import com.wiko.emarket.service.campaign.CampaignHistoryInfoService;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.emarket.vo.ApprovalListVo;
import com.wiko.emarket.vo.ApprovalQueryVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author shaofeng Guo
 * @Date 2022/6/6 14:59
 * @description: TODO
 **/
@Service
public class ApprovalServiceImpl implements ApprovalService {

    @Autowired
    private ApprovalDao approvalDao;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserRoleService userRoleService;

    @Autowired
    private BpmTaskService bpmTaskService;

    @Autowired
    private ProcessTaskInfoDao taskInfoDao;

    @Autowired
    private SysRoleService roleService;

    @Autowired
    private CampaignHistoryInfoService campaignHistoryInfoService;

    @Autowired
    private AcceptanceFormInfoService acceptanceFormInfoService;

    /**
     *  我的待办列表
     * @return
     */
    @Override
    public synchronized PageUtils list(ApprovalQueryVo approvalQueryVo) {
        // 获取当前登录用户id
        SysUserEntity user = (SysUserEntity)SecurityUtils.getSubject().getPrincipal();
        if (null == user) {
            throw new RRException("查询当前登录用户信息失败");
        }

        Integer currentPage = approvalQueryVo.getCurrentPage();
        Integer pageSize = approvalQueryVo.getPageSize();
        String status = approvalQueryVo.getStatus();
        Map<String,Object> map = new HashMap<>();
        // 对于status不同状态设置不同的值，
        // 1.已提交查询所有   2.已完成查询审批完成、  3.未完成查询提交中的
        List<String> statusList = new ArrayList<>();
        if ("08".equals(status)){
            // 1.1查询已完成(已完成，不通过)
            // 获取已完成流程id列表
            List<Integer> processIds = bpmTaskService.getDoneTask(user.getUsername());
            if (CollectionUtils.isEmpty(processIds)) {
                return new PageUtils(null, 0, pageSize, currentPage);
            }
            // 找到其任务数据
            List<ProcessTaskInfoVo> allTaskInfos = taskInfoDao.batchTasksByProcessIds(processIds);
            processIds = allTaskInfos.stream().filter(item -> ProcessTaskStatusEnum.HSITORY.getStatusCode().equals(item.getTaskStatus()))
                    .filter(item -> ProcessVisiableEnum.VISIABLE.getCode().equals(item.getVisiable()))
                    .filter(item -> StringUtils.equals(user.getUserId().toString(), item.getAssign()))
                    .map(ProcessTaskInfoVo::getProcessId).distinct().collect(Collectors.toList());
            // 排除节点处理人就是当前登录人的流程id
            List<Integer> filterProcessIds1 = allTaskInfos.stream().filter(item -> ProcessTaskStatusEnum.CURRENT.getStatusCode().equals(item.getTaskStatus()))
                    .filter(item -> StringUtils.equals(user.getUserId().toString(), item.getAssign()))
                    .map(ProcessTaskInfoVo::getProcessId).distinct().collect(Collectors.toList());

            processIds.removeAll(filterProcessIds1);
            if (CollectionUtils.isEmpty(processIds)) {
                return new PageUtils(null, 0, pageSize, currentPage);
            }
            map.put("ids",processIds);

        }else if ("09".equals(status)){
            // 1.2查询未完成(提交中)
            // 获取未完成流程id列表
            List<Integer> processIds = bpmTaskService.getTodoTask(user.getUsername());
            if (processIds.size() > 0){
                map.put("ids",processIds);
            }else {
                return new PageUtils(null, 0, pageSize, currentPage);
            }
        }else {
            // 1.3查提交所有
            map.put("createdBy",user.getUserId());
        }

        List<ApprovalListVo> totalList = approvalDao.selectTodoListById(map);
        map.put("currentPage",currentPage);
        map.put("pageSize",pageSize);
        // 查询campaign流程
        List<ApprovalListVo> approvalList = approvalDao.selectTodoListById(map);

        List<SysUserEntity> sysUserEntities = sysUserService.selectList(null);
        Map<Long, SysUserEntity> userMap = sysUserEntities.stream().collect(Collectors.toMap(SysUserEntity::getUserId, sysUserEntity -> sysUserEntity, (a, b) -> b));

        // 4.数据处理
        approvalList.forEach(approvalListVo -> {
            if (approvalListVo.getCampaignOwner() != null) {
                SysUserEntity userEntity = userMap.get(Long.valueOf(approvalListVo.getCampaignOwner()));
                String campaignOwner = (null == userEntity ? null : userEntity.getUserCard());
                approvalListVo.setCampaignOwner(campaignOwner);
            }
            Boolean isChinese=CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang());

            if(ProcessTypeEnum.EMARKET_APPLY.getProcessType().toString().equals(approvalListVo.getCampaignType())){
                approvalListVo.setStatus(isChinese? CampaignStatusEnum.getStatusNameCnByCode(approvalListVo.getStatus()) : CampaignStatusEnum.getStatusNameEnByCode(approvalListVo.getStatus()));
            }else{
                approvalListVo.setStatus(isChinese? AcceptanceFormStatusEnum.getStatusNameCnByCode(approvalListVo.getStatus()) : AcceptanceFormStatusEnum.getStatusNameEnByCode(approvalListVo.getStatus()));
            }
            Integer processtype = StringUtils.isEmpty(approvalListVo.getCampaignType()) ? null : Integer.parseInt(approvalListVo.getCampaignType());
            String campaignType = null == ProcessTypeEnum.getEnumByType(processtype) ? null : (isChinese? ProcessTypeEnum.getEnumByType(processtype).getProcessNameCn():ProcessTypeEnum.getEnumByType(processtype).getProcessNameEn());
            approvalListVo.setCampaignType(campaignType);

            if (approvalListVo.getCreatedBy() != null){
                SysUserEntity userEntity = userMap.get(Long.valueOf(approvalListVo.getCreatedBy()));
                String createdBy = (null == userEntity ? null : userEntity.getUserCard());
                approvalListVo.setCreatedBy(createdBy);
            }
        });
        // 5.返回分页数据
        return new PageUtils(approvalList, totalList.size(), pageSize, currentPage);
    }

    /**
     *  判断当前登录用户为提交人还是评审人
     * @return
     */
    @Override
    public R roleType() {
        SysUserEntity user = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
        // 1.查询登录用户角色id列表
        List<Long> idList = userRoleService.queryRoleIdList(user.getUserId());

        for (Long role : idList){
            // 2.通过roleId获取角色信息
            SysRoleEntity entity = roleService.selectById(role);

            // 3.是否提交人信息(提交人和评审人角色是不会冲突的，只能拥有其中一个)
            if (RoleNameEnum.CAMPAIGN_OWNER.getRoleNameCn().equals(entity.getRoleName())
                    || RoleNameEnum.CAMPAIGN_PA.getRoleNameCn().equals(entity.getRoleName())
            ){
                return R.ok().put("role","submit");
            }
            if (RoleNameEnum.BUSINESS_EXPERT.getRoleNameCn().equals(entity.getRoleName())
                    || RoleNameEnum.SIGNATORY.getRoleNameCn().equals(entity.getRoleName())
            ){
                return R.ok().put("role","approve");
            }
        }
        return R.error("该用户没有提交人或者业务专家的角色权限");
    }
}
