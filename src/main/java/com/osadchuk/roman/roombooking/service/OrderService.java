package com.osadchuk.roman.roombooking.service;

import com.osadchuk.roman.roombooking.entity.Order;
import com.osadchuk.roman.roombooking.entity.OrderStatus;
import com.osadchuk.roman.roombooking.entity.Room;
import com.osadchuk.roman.roombooking.entity.User;
import com.osadchuk.roman.roombooking.model.Booking;
import com.osadchuk.roman.roombooking.model.OrderDTO;
import com.osadchuk.roman.roombooking.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    private final UserService userService;

    @Autowired
    public OrderService(OrderRepository orderRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    public boolean checkIfRoomWillBeFree(long roomId, OrderDTO orderDTO) {
        Optional<Order> optionalOrder = orderRepository.findByRoomIdAndStatusIn(roomId,
                Arrays.asList(OrderStatus.CREATED, OrderStatus.IN_PROCESS, OrderStatus.APPROVED));

        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            LocalDate neededDate = LocalDate.parse(orderDTO.getBookingDate());
            LocalDate orderDate = order.getBookingDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            orderDate = orderDate.plusDays(order.getBookedDays());
            long daysBetweenDates = ChronoUnit.DAYS.between(orderDate, neededDate);

            if (neededDate.compareTo(orderDate) > 0) {
                return daysBetweenDates > order.getBookedDays();
            } else {
                return daysBetweenDates > orderDTO.getAmountOfDays();
            }
        } else return true;
    }

    public Order bookRoom(OrderDTO orderDTO, Optional<Room> room, Optional<User> user) {
        if (room.isPresent() && user.isPresent()) {
            Booking booking = Booking.getINSTANCE();
            booking.createOrder();
            booking.setUser(user.get());
            booking.setRoom(room.get());
            booking.setBookingDate(orderDTO.getBookingDate());
            booking.setBookedDays(orderDTO.getAmountOfDays());
            if (orderDTO.isIncludeBreakfast()) {
                booking.includeBreakfast();
            }
            Order order = booking.getOrder();
            return orderRepository.save(order);
        }
        return null;
    }
}
