package com.wiko.emarket.service.campaign.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.framework.common.utils.R;
import com.framework.common.utils.UuidUtil;
import com.wiko.emarket.dao.BenefitProdDao;
import com.wiko.emarket.entity.BenefitProd;
import com.wiko.emarket.service.campaign.BenefitProdService;
import com.wiko.emarket.util.MyConstant;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/27 13:59
 * @description: TODO
 **/
@Service("BenefitProdService")
public class BenefitProdServiceImpl extends ServiceImpl<BenefitProdDao, BenefitProd> implements BenefitProdService {

    @Override
    public List<BenefitProd> getProdByActId(String actId, Integer id) {
        Wrapper<BenefitProd> wrapper = new EntityWrapper<>();
        wrapper.eq("activity_id", actId);
        wrapper.eq("ref_id", id);
        wrapper.eq("status", MyConstant.DEL_STAUTS_NORMAL);
        return selectList(wrapper);
    }

    @Override
    public R save(BenefitProd benefitProd) {
        // 设置uuid
        benefitProd.setUuid(UuidUtil.get32UUID());
        benefitProd.setCreatedDate(new Date());
        benefitProd.setUpdatedDate(new Date());
        benefitProd.setStatus("1");
        Integer integer = baseMapper.insertAllColumn(benefitProd);
        if (integer > 0) {
            return R.ok("新增成功!");
        } else {
            return R.error("新增失败！");
        }
    }
}
