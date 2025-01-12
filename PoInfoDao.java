package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.PoInfo;
import com.wiko.emarket.entity.PoLineInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoInfoDao extends BaseMapper<PoInfo> {
    void insertPoLine(PoLineInfo poLineInfo);

    /**
     * 更新PO状态
     */
    void updatePoStatus (List<PoInfo> poInfoList);
}
