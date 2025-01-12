package com.wiko.emarket.controller;

import com.framework.common.utils.PageUtils;
import com.framework.common.utils.R;
import com.framework.modules.sys.controller.AbstractController;
import com.framework.modules.sys.entity.SysUserEntity;
import com.framework.modules.sys.service.SysUserService;
import com.wiko.emarket.entity.AreaEntity;
import com.wiko.emarket.service.campaign.AreaService;
import com.wiko.emarket.service.campaign.CampaignService;
import com.wiko.emarket.service.campaign.CampaignHistoryInfoService;
import com.wiko.emarket.vo.CampaignCreateVo;
import com.wiko.emarket.vo.CampaignHistoryInfoVo;
import com.wiko.emarket.vo.UserLikeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Author shaofeng Guo
 * @Date 2022/4/28 17:08
 * @description: TODO
 **/
@RestController
@RequestMapping("/api/campaign")
@Slf4j
public class CampaignController extends AbstractController {
    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignHistoryInfoService campaignHistoryInfoService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private AreaService areaService;

    @RequestMapping("/list")
    public R list(@RequestBody Map<String,Object> params){
        PageUtils page;
        try {
            Long userId = this.getUser().getUserId();
            page = campaignHistoryInfoService.list(params,userId);
        } catch (Exception e) {
            log.error("getCampaignList error", e);
            return R.error("查询Campaign列表失败");
        }
        return R.ok().put("page",page);
    }

    /**
     * campaign详情
     *
     * @param detailScene
     * @param campaignId
     * @param id
     * @return
     */
    @RequestMapping("/detail/{detailScene}/{campaignId}/{id}")
    public R detail(@PathVariable("detailScene") String detailScene, @PathVariable("campaignId") String campaignId,
                    @PathVariable("id") Integer id){
        CampaignHistoryInfoVo campaignHistoryInfoVo = campaignHistoryInfoService.queryCampaignHisInfo(detailScene,
                campaignId, id);
        return R.ok().put("campaignVo",campaignHistoryInfoVo);
    }

    /**
     * 提交
     *
     * @param campaignCreateVo Campaign创建界面提交数据
     * @return R
     */
    @RequestMapping("/create")
    public R create(@RequestBody CampaignCreateVo campaignCreateVo){
        return campaignService.create(campaignCreateVo);
    }

    /**
     * 保存
     *
     * @param campaignCreateVo Campaign创建界面保存数据
     * @return R
     */
    @RequestMapping("/save")
    public R save(@RequestBody CampaignCreateVo campaignCreateVo){
        return campaignService.save(campaignCreateVo);
    }

    @RequestMapping("/delete")
    public R delete(@RequestBody Map<String, Object> params){
        String campaignId = (String) params.get("campaignId");
        Integer id = Integer.parseInt(params.get("id").toString());
        return campaignService.deleteByCampaignId(campaignId, id);
    }

    /**
     * 模糊查询用户接口
     * @return
     */
    @RequestMapping("/likeCampaignOwner")
    public R likeCampaignOwner(@RequestParam String roleName){
        List<UserLikeVo> list =  userService.selectLike(roleName);
        return R.ok().put("list",list);
    }

    /**
     * 模糊查询用户接口
     * @return
     */
    @RequestMapping("/getUser")
    public R getUserByRoleNameAndName(@RequestParam String roleName,@RequestParam String name){
        List<UserLikeVo> list = userService.getUserByRoleNameAndName(roleName,name);
        return R.ok().put("list",list);
    }

    /**
     * 三级分类树
     */
    @RequestMapping("/list/tree")
    public R listWithTree(){
        List<AreaEntity> list;
        try {
            Long userId = this.getUser().getUserId();
            list = areaService.listWithTree1(userId);
        } catch (Exception e){
            log.error("Get AreaListWithTree error", e);
            return R.error("查询地区树失败");
        }
        return R.ok().put("data",list);
    }


    /**
     * 三级分类树
     */
    @RequestMapping("/list/treeEdit")
    public R listWithTreeEdit(){
        List<AreaEntity> list;
        try {
            list = areaService.listWithTree();
        } catch (Exception e){
            log.error("Get AreaListWithTree error", e);
            return R.error("查询地区树失败");
        }
        return R.ok().put("data",list);
    }

    /**
     * 复制
     */
    @RequestMapping("/copy/{campaignId}/{id}")
    public R copy(@PathVariable("campaignId") String campaignId, @PathVariable("id") Integer id){
        CampaignHistoryInfoVo campaignVo = campaignService.copyCampaign(campaignId, id);
        return R.ok().put("campaignVo",campaignVo);
    }

    /**
     *  查询pr详情
     * @param map
     * @return
     */
    @RequestMapping("/getPrDetails")
    public R getPrDetails(@RequestBody Map<String,Object> map){
        return campaignService.getPrDetails(map);
    }

    /**
     *  查询po详情
     * @param map
     * @return
     */
    @RequestMapping("/getPoDetails")
    public R getPoDetails(@RequestBody Map<String,Object> map){
        return campaignService.getPoDetails(map);
    }

    /**
     * 查询待办转移人员列表
     * @return
     */
    @RequestMapping("/getTransferUsers")
    private R getTransferUsers(@RequestBody Map<String, Object> params){
        try {
            return R.ok().put("data", campaignService.getTransferUsers(params));
        } catch (Exception e) {
            log.error("getTransferUsers error", e);
            return R.error("get transfer Users faield");
        }
    }

    /**
     * 查询campaign执行信息
     *
     * @return R
     */
    @RequestMapping("/queryCampaignExecuteInformation")
    private R queryCampaignExecuteInformation(@RequestBody Map<String, Object> params) {
        try {
            Long userId = this.getUser().getUserId();
            return R.ok().put("data", campaignHistoryInfoService.queryCampaignExecuteInformation(params, userId));
        } catch (Exception e) {
            log.error("queryCampaignExecuteInformation error", e);
            return R.error("query campaign execute information failed");
        }
    }

    /**
     * 导出campaign执行信息
     *
     * @param params
     * @param response
     */
    @RequestMapping("/exportCampaignExecuteInformation")
    private void exportCampaignExecuteInformation(@RequestParam Map<String, Object> params,
                                                  HttpServletResponse response) {
        try {
            Long userId = this.getUser().getUserId();
            campaignHistoryInfoService.exportCampaignExecuteInformation(params, userId, response);
        } catch (Exception e) {
            log.error("exportCampaignExecuteInformation error", e);
        }
    }

    /**
     * 查询开票金额详情
     *
     * @param map
     * @return R
     */
    @RequestMapping("/getPoBillingedDetails")
    public R getPoBillingedDetails(@RequestBody Map<String, Object> map) {
        return campaignHistoryInfoService.getPoBillingedDetails(map);
    }

    /**
     * 查询付记录详情
     *
     * @param map
     * @return R
     */
    @RequestMapping("/getPoPaymentedDetails")
    public R getPoPaymentedDetails(@RequestBody Map<String, Object> map) {
        return campaignHistoryInfoService.getPoPaymentedDetails(map);
    }

    /**
     * l2回冲更新逻辑
     *
     * @param map
     * @return R
     */
    @RequestMapping("/updateL2Recovery")
    public R updateL2Recovery(@RequestBody Map<String, Object> map) {
        SysUserEntity currentUser = this.getUser();
        return campaignHistoryInfoService.updateL2Recovery(map, currentUser);
    }


    /**
     * campaign回冲更新逻辑
     *
     * @param map
     * @return R
     */
    @RequestMapping("/getCampaignRecoveryDialogInfo")
    public R getLv2RecoveryInfo(@RequestBody Map<String, Object> map) {
        SysUserEntity currentUser = this.getUser();
        return campaignHistoryInfoService.getCampaignRecoveryDialogInfo(map, currentUser);
    }

    /**
     * campaign回冲更新逻辑
     *
     * @param map
     * @return R
     */
    @RequestMapping("/updateCampaignRecovery")
    public R updateCamapignRecovery(@RequestBody Map<String, Object> map) {
        SysUserEntity currentUser = this.getUser();
        return campaignHistoryInfoService.updateCampaignRecovery(map, currentUser);
    }
}
