package com.wiko.emarket.service.campaign;

import com.baomidou.mybatisplus.service.IService;
import com.framework.common.utils.R;
import com.framework.modules.sys.entity.SysDictEntity;
import com.wiko.emarket.entity.ActivityType;

import java.util.List;
import java.util.Map;

public interface ActivityTypeService extends IService<ActivityType> {
    List<ActivityType> listWithTree(Map<String, Object> param);

    /**
     * 获取采购方式
     *
     * @return
     */
    List<SysDictEntity> getPurchaseType(Map<String, Object> param);

    List<ActivityType> getActivityType(String level, String typeName);

    String updateActivityType(ActivityType activityType,Long userId);

    String deleteActivityType(ActivityType activityType);

    String insertActivityType(ActivityType activityType,Long userId);

    List<ActivityType> queryActivityType();

    R queryActivityTypeTree();


    /**
     * 根据level,parentid查询对应层级的活动类型
     * @param params
     * @return R
     */
    R queryActivityTypeByLevel(Map<String,Object> params);

    R queryActivityTypeTreeForReviewer(String areaCode);
}
