package com.shopeasy.dto.request;

import com.shopeasy.model.OrderStatus;
import lombok.Data;

@Data
public class OrderStatusRequest {
    private OrderStatus status;
}
