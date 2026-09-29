package com.tianyi.railticket.controller;

import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.dto.OrderCreateDTO;
import com.tianyi.railticket.dto.OrderPayDTO;
import com.tianyi.railticket.dto.OrderRefundDTO;
import com.tianyi.railticket.service.OrderService;
import com.tianyi.railticket.vo.OrderCreateVO;
import com.tianyi.railticket.vo.OrderPayVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public Result<OrderCreateVO> createOrder(@Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        return Result.ok(orderService.create(orderCreateDTO));
    }

    /** 退票：待支付单 → 用户取消，已支付单 → 已退票，仅本人订单可退 */
    @PostMapping("/refund")
    public Result<Void> refund(@Valid @RequestBody OrderRefundDTO orderRefundDTO) {
        orderService.refund(orderRefundDTO);
        return Result.ok();
    }

    /** 支付（模拟渠道）：仅待支付且未超时的本人订单可付，重复支付返回 40010，超时支付返回 40011 */
    @PostMapping("/pay")
    public Result<OrderPayVO> pay(@Valid @RequestBody OrderPayDTO orderPayDTO) {
        return Result.ok(orderService.pay(orderPayDTO));
    }

    /** 手动触发超时关单（运维/调试用，正常由每分钟的定时任务执行），返回关闭笔数 */
    @PostMapping("/close-timeout")
    public Result<Integer> closeTimeout(@RequestParam(defaultValue = "200") int limit) {
        return Result.ok(orderService.closeTimeoutOrders(limit));
    }
}
