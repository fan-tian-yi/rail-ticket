package com.tianyi.railticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_train")
public class TrainDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String trainNo;
    private Integer trainType;
    private Integer status;
    private String seatConfig;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
