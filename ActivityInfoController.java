package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.emarket.service.campaign.ActivityInfoService;
import com.wiko.emarket.vo.UserLikeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author linjian
 * date: 2022/4/27
 */
@RestController
@RequestMapping("/api/activityInfos")
public class ActivityInfoController extends AbstractController {

    @Autowired
    private ActivityInfoService activityInfoService;

    @Autowired
    private SysUserService userService;

    /**
     * 模糊查询活动执行人
     * @return
     */
    @RequestMapping("/likeActExecutor")
    public R likeCampaignOwner(@RequestParam String roleName, @RequestParam(required = false,value = "userId") String userId){
        List<UserLikeVo> list =  userService.selectLikeIncludeUserId(roleName, userId);
        return R.ok().put("list",list);
    }

    @RequestMapping("/getProds")
    public R getProds(@RequestParam(required = false) String roleName){
        return R.ok().put("list",activityInfoService.getBenefitProdByDict());
    }
}
