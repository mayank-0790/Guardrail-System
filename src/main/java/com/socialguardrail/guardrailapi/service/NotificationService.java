package com.socialguardrail.guardrailapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NotificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int Cooldown=15;

    public void handleNotification(Long userId,String message){
        System.out.println("DEBUG: handleNotification called");
        String cooldownKey="user:"+userId+":notif_cooldown";
        String listKey="user:"+userId+":pending_notifs";
        String activeSet="notif:active_users";

        boolean hasCooldown=Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));

        if(hasCooldown) {
            redisTemplate.opsForList().rightPush(listKey, message);

            redisTemplate.opsForSet().add(activeSet, userId.toString());
        }else{
            System.out.println("Notification Sent "+ userId +":"+message);

            redisTemplate.opsForValue().set(cooldownKey,"1", Duration.ofMinutes(Cooldown));
        }
    }
}
