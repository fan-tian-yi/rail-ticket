package com.tianyi.railticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_train")
public class Train {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String trainNo;
    private Integer trainType;
    private Integer status;
    private String seatConfig;
}
