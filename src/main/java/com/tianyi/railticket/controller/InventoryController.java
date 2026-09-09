package com.tianyi.railticket.controller;

import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.dto.AvailableQueryDTO;
import com.tianyi.railticket.entity.model.SegRange;
import com.tianyi.railticket.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/available")
    public Result<Integer> available(@Valid AvailableQueryDTO dto) {
        SegRange range = inventoryService.resolveRange(
                dto.getTrainId(), dto.getFromStationId(), dto.getToStationId());
        return Result.ok(inventoryService.getAvailable(
                dto.getTrainId(), dto.getTrainDate(), dto.getSeatType(),
                range.getFromSeq(), range.getToSeq()));
    }

    @PostMapping("/warm-up")
    public Result<Integer> warmUp() { return Result.ok(inventoryService.warmUp()); }
}
