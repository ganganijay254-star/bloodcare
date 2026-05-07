package com.bloodcare.bloodcare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bloodcare.bloodcare.entity.Reward;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    
    List<Reward> findByUser(User user);
    
    List<Reward> findByUserAndStatus(User user, String status);
    
    Reward findByRewardCode(String rewardCode);

    Reward findByVisitRequest(VisitRequest visitRequest);

    boolean existsByVisitRequest(VisitRequest visitRequest);

    void deleteByUser(User user);
}
