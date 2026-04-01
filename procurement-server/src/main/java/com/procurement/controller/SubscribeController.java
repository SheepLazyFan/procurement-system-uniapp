package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.entity.SysUser;
import com.procurement.mapper.UserMapper;
import com.procurement.scheduler.StockWarningScheduler;
import com.procurement.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅消息控制器
 * <p>
 * 提供手动触发库存预警推送的测试接口。
 * 前端订阅授权完全在小程序端通过 wx.requestSubscribeMessage 完成，
 * 不需要后端接口。
 * </p>
 */
@RestController
@RequestMapping("/subscribe")
@RequiredArgsConstructor
public class SubscribeController {

    private final StockWarningScheduler stockWarningScheduler;
    private final UserMapper userMapper;

    /**
     * 查询当前用户库存预警通知开关状态
     */
    @GetMapping("/notify/status")
    public R<Boolean> getNotifyStatus(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        return R.ok(user != null && Integer.valueOf(1).equals(user.getNotifyStockWarning()));
    }

    /**
     * 开启库存预警通知
     */
    @PostMapping("/notify/enable")
    public R<String> enableNotify(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser update = new SysUser();
        update.setId(loginUser.getUserId());
        update.setNotifyStockWarning(1);
        userMapper.updateById(update);
        return R.ok("通知已开启");
    }

    /**
     * 关闭库存预警通知
     */
    @PostMapping("/notify/disable")
    public R<String> disableNotify(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser update = new SysUser();
        update.setId(loginUser.getUserId());
        update.setNotifyStockWarning(0);
        userMapper.updateById(update);
        return R.ok("通知已关闭");
    }

    /**
     * 手动触发库存预警推送（测试用）
     * <p>
     * 立即检查当前企业的库存预警并推送通知给企业主。
     * force=true 时跳过 Redis 去重，允许今天重复推送，方便反复测试。
     * </p>
     */
    @PostMapping("/stock-warning/trigger")
    public R<String> triggerStockWarning(@AuthenticationPrincipal LoginUser loginUser,
                                         @RequestParam(defaultValue = "false") boolean force) {
        stockWarningScheduler.triggerManually(loginUser.getEnterpriseId(), force);
        return R.ok("库存预警推送已触发，请检查微信服务通知");
    }
}
