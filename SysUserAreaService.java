package com.wiko.emarket.service.campaign;

import com.baomidou.mybatisplus.service.IService;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.SysUserArea;
import com.wiko.emarket.entity.SysUserAreaParam;

import java.util.List;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:51
 */
public interface SysUserAreaService extends IService<SysUserArea> {

    String saveUserArea(List<SysUserAreaParam> newList, List<String> oldList,Long userId);

    List<AreaEntity> getCtyCodeByUserId(Long userId);

    List<String> getUserArea(Long userId);
}
