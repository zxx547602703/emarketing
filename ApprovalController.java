package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.wiko.emarket.service.campaign.ApprovalService;
import com.wiko.emarket.vo.ApprovalQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @Author shaofeng Guo
 * @Date 2022/6/6 14:55
 * @description: TODO
 **/
@RestController
@RequestMapping("/api/approval")
@Slf4j
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @RequestMapping("/list")
    public R approvalList(@RequestBody ApprovalQueryVo approvalQueryVo){
        PageUtils list = approvalService.list(approvalQueryVo);
        return R.ok().put("list",list);
    }
    @RequestMapping("/roleType")
    public R roleType(){
        return approvalService.roleType();
    }
}
