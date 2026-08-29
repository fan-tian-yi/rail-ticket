package com.tianyi.railticket.controller;

import com.tianyi.railticket.common.Result;
import com.tianyi.railticket.dto.TrainQueryDTO;
import com.tianyi.railticket.service.TrainQueryService;
import com.tianyi.railticket.vo.TrainItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/trains")
@RequiredArgsConstructor
public class TrainQueryController {
    private final TrainQueryService trainQueryService;

    @GetMapping
    public Result<List<TrainItemVO>> queryTrain(@Valid TrainQueryDTO dto) {
        return Result.ok(trainQueryService.query(dto));
    }
}
