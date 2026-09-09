package com.tianyi.railticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TrainQueryDTO {

    @NotNull(message = "出发站不能为空")
    private Long fromStationId;

    @NotNull(message = "到达站不能为空")
    private Long toStationId;

    @NotNull(message = "乘车日期不能为空")
    private LocalDate trainDate;
}
