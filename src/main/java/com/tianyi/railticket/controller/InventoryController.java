package com.tianyi.railticket.controller;

import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.entity.model.SegRange;
import com.tianyi.railticket.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/available")
    public Result<Integer> available(@RequestParam Long trainId,
                                     @RequestParam String trainDate,
                                     @RequestParam Long fromStationId,
                                     @RequestParam Long toStationId,
                                     @RequestParam Integer seatType) {
        LocalDate date = LocalDate.parse(trainDate);
        SegRange range = inventoryService.resolveRange(trainId, fromStationId, toStationId);
        return Result.ok(inventoryService.getAvailable(trainId, date, seatType, range.getFromSeq(), range.getToSeq()));
    }

    // B：POST 无参数，永远用配置里的默认值
    @PostMapping("/warm-up")
    public Result<Integer> warmUp() { return Result.ok(inventoryService.warmUp()); }
}
