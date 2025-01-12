package com.wiko.emarket.dao;

import com.wiko.emarket.vo.ApprovalListVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalDao {

    /**
     * 待办查询流程
     *
     * @param map
     * @return
     */
    List<ApprovalListVo> selectTodoListById(Map<String,Object> map);
}
