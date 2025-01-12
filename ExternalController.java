package com.wiko.emarket.controller.foreign;

import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.service.foreign.ActivityTypeQueryService;
import com.wiko.emarket.vo.foreign.ActivityTypeQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外接口
 *
 */
@Slf4j
@RestController
@RequestMapping("/foreign")
public class ExternalController extends AbstractController {
    @Autowired
    private ActivityTypeQueryService activityTypeQueryService;

    /**
     * 查询业务科目
     *
     * @return R
     */
    @RequestMapping("/activitytype/list")
    public R queryActivityType(@RequestBody (required =false)ActivityTypeQueryVo activityTypeQueryVo){
        log.debug("foreign queryActivityType RequestBody:{}", activityTypeQueryVo);
        if (null == activityTypeQueryVo || null == activityTypeQueryVo.getAppId()) {
            return R.error("请求体或者appId 值不能为空");
        }
        if (!"BPM".equalsIgnoreCase(activityTypeQueryVo.getAppId())) {
            return R.error("无权调用，请联系MKT系统管理员");
        }

        return R.ok().put("data",activityTypeQueryService.queryActivityTypeByParam(activityTypeQueryVo));
    }
}
