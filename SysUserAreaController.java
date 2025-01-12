package com.wiko.emarket.controller;

import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.SysUserAreaService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:49
 */
@RestController
@RequestMapping("/api")
public class SysUserAreaController extends AbstractController {
    @Resource
    private SysUserAreaService service;

    @Resource
    private AreaService areaService;

//    @PostMapping("/user/area/")
//    public R saveUserArea(@RequestBody List<SysUserAreaParam> list){
//        String retStr = service.saveUserArea(, );
//        if("success".equals(retStr)) {
//            return R.ok();
//        }else{
//            return R.error(100,retStr);
//        }
//
//    }

    @PostMapping("/user/area/list")
    public List<AreaEntity> getCtyCodeByUserId(@RequestParam("level") String level){
        List<AreaEntity> ctyCodeByUserId = areaService.getAllAreaList(getUserId(), level);
       return ctyCodeByUserId;
    }

    @GetMapping("/singleUser/area/list")
    public List<AreaEntity> getCtyCodeByUserId(@RequestParam("userId") Long userId){
        List<AreaEntity> ctyCodeByUserId = areaService.getAllAreaList(userId,"3");
        return ctyCodeByUserId;
    }
}
