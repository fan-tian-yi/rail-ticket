package com.tianyi.railticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianyi.railticket.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {

    /** CAS 改状态：仅当库中状态仍为 expectedStatus 时生效；返回影响行数（1=抢到所有权，0=已被改） */
    @Update("""
            UPDATE t_order
            SET status = #{newStatus}, cancel_time = #{cancelTime}
            WHERE order_no = #{orderNo} AND status = #{expectedStatus}
            """)
    int casStatus(@Param("orderNo") String orderNo,
                  @Param("expectedStatus") int expectedStatus,
                  @Param("newStatus") int newStatus,
                  @Param("cancelTime") LocalDateTime cancelTime);

    /** 查已到期但仍待支付的订单（走 idx_order_status_expire）；一次取全关单所需字段，免去逐单再查 */
    @Select("""
            SELECT id, order_no, train_id, train_date, seat_type
            FROM t_order
            WHERE status = 0 AND expire_time < #{now}
            ORDER BY expire_time
            LIMIT #{limit}
            """)
    List<OrderDO> selectTimeoutOrders(@Param("now") LocalDateTime now,
                                      @Param("limit") int limit);
}
