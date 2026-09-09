package com.tianyi.railticket.controller;

import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.dto.OrderCreateDTO;
import com.tianyi.railticket.service.OrderService;
import com.tianyi.railticket.vo.OrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
