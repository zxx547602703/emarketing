package com.wiko.emarket.service.campaign;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.wiko.emarket.vo.ApprovalQueryVo;

/**
 * 我的待办service
 *
 * @Author shaofeng Guo
 * @Date 2022/6/6 14:58
 * @description: TODO
 **/

public interface ApprovalService {
    PageUtils list(ApprovalQueryVo approvalQueryVo);

    R roleType();
}
