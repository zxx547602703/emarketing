package com.wiko.emarket.dao;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.wiko.emarket.entity.PoLineInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PoLineInfoDao extends BaseMapper<PoLineInfo> {
    List<String> selectList1(@Param("poId") String poId);

    BigDecimal queryAcceptanceAmount(String poId);
}
