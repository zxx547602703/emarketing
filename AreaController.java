package com.wiko.emarket.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.wiko.emarket.constant.CommonConstant;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.entity.BudgetTypeEntity;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.SysUserAreaService;
import com.wiko.emarket.util.RequestUtil;
import com.wiko.psi.entity.Country;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ZhangYi
 * @title:
 * @projectName: PSI
 * @description: TODO
 * @date: 2022/4/29 16:49
 */
@RestController
@RequestMapping("/api")
public class AreaController extends AbstractController {
    @Resource
    private AreaService service;

    @Resource
    private SysUserAreaService sysUserAreaService;

    @RequestMapping ("/area")
    public JSONObject getAreaTree(@RequestParam Map<String, String> params){
        JSONObject areaTree = service.getAreaTree(params.get("code"),params.get("level"));
        JSONObject retJson = new JSONObject();
        if(null != areaTree){
            retJson.put("data",areaTree);
            retJson.put("msg", "success");
            retJson.put("code", 0);
        } else {
            retJson.put("data","");
            retJson.put("msg", "get data is null");
            retJson.put("code",500);
        }
        return retJson;
    }

    /**
     * 评审人权签人选择所属区域
     * @return
     */
    @RequestMapping ("/user/area")
    public JSONObject getAreaTreeByUser(){
        JSONObject retJson = service.getUserAreaTree(this.getUserId());
        return retJson;
    }

    /**
     * 提交人所属区域
     * @return
     */
    @RequestMapping ("/compaign/getAreaTreeForSubmiter")
    public JSONArray getAreaTreeForSubmiter(){
        JSONArray retJson = service.getAreaTreeForSubmiter(this.getUserId());
        return retJson;
    }

    /**
     * campaign创建中预算资金池地区部组织
     * @return
     */
    @RequestMapping ("/compaign/getBudgetOrg")
    public JSONObject getBudgetOrg(@RequestParam("rgCode")String rgCode){
        JSONObject retJson = service.getBudgetOrg(rgCode);
        return retJson;
    }

    /**
     * campaign创建中预算资金池代表处组织
     * @return
     */
    @RequestMapping ("/compaign/getBudgetRO")
    public JSONObject getBudgetRO(@RequestParam("rgCode")String rgCode, @RequestParam("submitterRo")String submitterRo){
        JSONObject retJson = service.getBudgetRO(rgCode, submitterRo);
        return retJson;
    }

    /**
     * campaign创建中预算资金池组织的国家
     * @return
     */
    @RequestMapping ("/compaign/getBudgetCountry")
    public List<AreaEntity> getBudgetCountry(@RequestParam("roCode")String roCode){
        List<AreaEntity> budgetCountry = service.getBudgetCountry(roCode);
        return budgetCountry;
    }

    /**
     * campaign创建中预算类型
     * @return
     */
    @RequestMapping ("/compaign/getBugetType")
    public List<BudgetTypeEntity> getBugetType(@RequestParam("roCode")String roCode, @RequestParam("submiterRoCode")String submiterRoCode){
        List<BudgetTypeEntity> bugetType = service.getBugetType(roCode, submiterRoCode);
        return bugetType;
    }

    /**
     * campaign创建中预算年度
     * @return
     */
    @RequestMapping ("/compaign/getBugetYear")
    public List<String> getBugetYear(){
        List<String> bugetYears = service.getBugetYear();

        return bugetYears;
    }


    @GetMapping("/area/fee/{code}/{year}")
    public JSONArray getAreaFee(@PathVariable("code") String code,
                               @PathVariable("year") String year){
        JSONArray array =new JSONArray();
        array = service.getAreaFee(code, year);
        return array;
    }

    @GetMapping("/single/area/fee/{code}/{year}")
    public JSONArray getSingleAreaFee(@PathVariable("code") String code,
                                 @PathVariable("year") String year){
        JSONArray array = service.getSingleAreaFee(code, year);
        return array;
    }


    /**
     * 查询用户所属区域，只包括所有国家级level=4
     */
//    @SysLog("查询用户所属区域，包括所有国家层级level=4")
    @GetMapping("/all/area/{code}")
    public List<AreaEntity>  getAllAreaCode(@PathVariable("code") String code){
        List<AreaEntity> list = service.getAllAreaCode(code);
        return list;
    }

    /**
     * 查询区域Map
     */
//    @SysLog("查询区域Map)
    @GetMapping("/all/area/map")
    public Map<String,String> getAllAreaMap(){
        Map<String,String> map = new HashMap<>();
        List<AreaEntity> list = service.getAllArea();
        if(CommonConstant.ZH_LANGUAGE.equals(RequestUtil.getLang())){
            map = list.stream().collect(Collectors.toMap(AreaEntity::getCode,AreaEntity::getNameCn,(k1,k2)->k1));
        }
        else{
            map = list.stream().collect(Collectors.toMap(AreaEntity::getCode,AreaEntity::getNameEn,(k1,k2)->k1));
        }
        return map;
    }

    /**
     * 查询区域list
     */
//    @SysLog("查询区域list")
    @GetMapping("/all/area")
    public List<AreaEntity>  getAllArea(){
        List<AreaEntity> list = service.getAllArea();
        return list;
    }

    /**
     * 查询用户所属区域(地区级以上)
     */
//    @SysLog("查询用户所属区域，包括所有层级")
    @GetMapping("/all/area/list")
    public List<AreaEntity> getAllAreaList(@RequestParam("level") String level){
        List<AreaEntity> list = service.getAllAreaList(getUser().getUserId(), level);
        list.remove(null);
        return list;
    }

    /**
     * 查询预算树（菜单预算管理查询）
     * @param year
     * @return
     */
    @GetMapping("/area/fee/tree/{year}")
    public JSONArray getAllAreaFeeTree(@PathVariable("year") String year){
        JSONArray allAreaFeeTree = service.getAllAreaFeeTree(year, this.getUser().getUserId());
        return allAreaFeeTree;
    }

    @RequestMapping("/area/fee/updateAreaTree")
    public R updateAreaTree(@RequestBody AreaEntity areaEntity) {
        return service.updateAreaTree(areaEntity);
    }
    /**
     * @Description //TODO删除区域节点子节点
     * @Param [areaEntity]
     * @return com.framework.common.utils.R
     **/
    @RequestMapping("/area/fee/deleteAreaTree")
    public R deleteAreaTree(@RequestBody AreaEntity areaEntity) {
        return service.deleteAreaTree(areaEntity);
    }
    /**
     * @Description //TODO 新增区域节点
     * @Param [areaEntity]
     * @return com.framework.common.utils.R
     **/
    @RequestMapping("/area/fee/insertAreaTree")
    public R insertAreaTree(@RequestBody AreaEntity areaEntity) {
        areaEntity.setLevel(String.valueOf(Integer.valueOf(areaEntity.getLevel())+1));
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        areaEntity.setCreatedDate(simpleFormat.format(new Date()));
        return service.insertAreaTree(areaEntity);
    }
    
    /**
     * @Description //TODO 构建国家下拉框
     * @Param []
     * @return java.util.List<com.wiko.psi.entity.Country>
     **/
    @RequestMapping("/area/fee/countryList")
    public List<Country> countryList() {
        return service.queryCountryList();
    }

}
