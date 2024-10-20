package com.aeterna.friendmall.seckill;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//@RunWith((SpringRunner.class))
//@SpringBootTest
public class FriendmallSeckillApplicationTests {

    @Test
    public void contextLoads() {
        LocalDate now = LocalDate.now();
        System.out.println("now = " + now);
        LocalDate plus = now.plusDays(1);
        System.out.println("plus = " + plus);
        LocalDate plus2 = now.plusDays(2);
        System.out.println("plus2 = " + plus2);

        LocalTime min = LocalTime.MIN;
        System.out.println("min = " + min);
        LocalTime max = LocalTime.MAX;
        System.out.println("max = " + max);

        LocalDateTime start = LocalDateTime.of(now, min);
        System.out.println("start = " + start);
        LocalDateTime end = LocalDateTime.of(plus2, max);
        System.out.println("end = " + end);

        // 日期格式化
        String format = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("format = " + format);

    }

}
