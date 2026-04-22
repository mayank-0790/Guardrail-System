package com.socialguardrail.guardrailapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class NotificationSchedular {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 20000)
    public void sweepNotifications(){

        String activeSet="notif:active_users";
        Set<String> users=redisTemplate.opsForSet().members(activeSet);

        if(users==null|| users.isEmpty()){
            return;
        }
        for(String userId:users){
            String listKey="user:"+userId+":pending_notifs";
            Long size=redisTemplate.opsForList().size(listKey);
            if(size!=null && size>0){
               String first=redisTemplate.opsForList().leftPop(listKey);
               long remaining=size-1;

               System.out.println("Notification for user"+ userId+": "+first+
                       (remaining>0?"and"+remaining+"others interacted with your posts.":""));
               redisTemplate.delete(listKey);
            }
            redisTemplate.opsForSet().remove(activeSet,userId);
        }
    }
}
