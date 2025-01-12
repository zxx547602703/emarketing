package com.wiko.emarket.controller;

import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.entity.BenefitProd;
import com.wiko.emarket.service.campaign.BenefitProdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/27 11:53
 * @description: TODO
 **/
@RestController
@RequestMapping("/api/benefitProd")
public class BenefitProdController extends AbstractController {
    @Autowired
    private BenefitProdService benefitProdService;

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody BenefitProd benefitProd){
//        benefitProd.setCreatedBy(getUser().getUsername());
//        benefitProd.setUpdatedBy(getUser().getUsername());
        benefitProdService.save(benefitProd);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete/{uuid}")
    public R delete(@PathVariable("uuid") String uuid){
        boolean b = benefitProdService.deleteById(uuid);
        if (b) {
            return R.ok("删除成功！");
        }else {
            return R.error("删除失败！");
        }
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody BenefitProd benefitProd){
        benefitProd.setUpdatedDate(new Date());
        boolean b = benefitProdService.updateById(benefitProd);
        if (b) {
            return R.ok("修改成功！");
        }else {
            return R.ok("无更新数据！");
        }
    }
}
