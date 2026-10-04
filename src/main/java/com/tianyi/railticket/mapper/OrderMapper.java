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

    /** CAS 支付：仅当仍是待支付**且未过有效期**才生效；返回影响行数（1=支付成功，0=已支付/已关闭/已超时）*/
    @Update("""
            UPDATE t_order
            SET status = 1, pay_time = #{payTime}
            WHERE order_no = #{orderNo} AND status = 0 AND expire_time > #{payTime}
            """)
    int casPay(@Param("orderNo") String orderNo, @Param("payTime") LocalDateTime payTime);

    /**
     * 查「已到期且过了宽限期」仍待支付的订单（now 传的是 expire_time + 宽限 的时间点）。
     * 走 idx_order_status_expire；一次取全关单所需字段，免去逐单再查。
     */
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
