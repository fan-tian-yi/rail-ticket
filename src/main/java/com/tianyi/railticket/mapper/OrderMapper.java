package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
