package com.wiko.emarket.service.campaign.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.wiko.emarket.dao.SysUserAreaDao;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.SysUserArea;
import com.wiko.emarket.entity.SysUserAreaParam;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.SysUserAreaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:52
 */
@Service
public class SysUserAreaServiceImpl extends ServiceImpl<SysUserAreaDao, SysUserArea>  implements SysUserAreaService {

    @Resource
    private AreaService areaService;

    @Override
    public String saveUserArea(List<SysUserAreaParam> newList, List<String> list,Long userId) {
        Wrapper<SysUserArea> wrapper =new EntityWrapper<>();
        wrapper.eq("user_id",userId);
        this.delete(wrapper);
        List<SysUserArea> sysUserAreas = new ArrayList<>();
        if (null != newList && newList.size()>0 ) {
            for(SysUserAreaParam sysUserAreaParam: newList){
                SysUserArea sysUserArea = new SysUserArea();
                sysUserArea.setUserId(userId);
                sysUserArea.setAreaCode(sysUserAreaParam.getCode());
                sysUserArea.setLevel(sysUserAreaParam.getLevel());
                sysUserArea.setCreatedDate(DateUtil.formatDate(DateUtil.date()));
                sysUserAreas.add(sysUserArea);
            }
        }
        try {
            this.insertBatch(sysUserAreas);
        } catch (Exception e) {
            return "update data fail";
        }
        return "success";
    }

    @Override
    public List<AreaEntity> getCtyCodeByUserId(Long userId) {
        EntityWrapper<SysUserArea> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId);
        List<SysUserArea> sysUserAreas = this.selectList(wrapper);
        List<AreaEntity> list = new ArrayList<>();
        for(SysUserArea sysUserArea : sysUserAreas){
            list.addAll(areaService.getAllAreaCode(sysUserArea.getAreaCode()));
        }
        return list;
    }

    @Override
    public List<String> getUserArea(Long userId) {
        List<String> list = new ArrayList<>();
        EntityWrapper<SysUserArea> wrapper = new EntityWrapper<>();
        wrapper.eq("user_id", userId).in("level", "1,2,3");
        List<SysUserArea> sysUserAreas = this.selectList(wrapper);
        for(SysUserArea area:sysUserAreas ){
            list.add(area.getAreaCode());
        }
        return list;
    }
}
