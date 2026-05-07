package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bloodcare.bloodcare.entity.Reward;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.RewardRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REDEEMED = "REDEEMED";
    private static final List<Map<String, Object>> REWARD_DEFINITIONS = new ArrayList<>();

    static {
        REWARD_DEFINITIONS.add(createRewardDef("Free Full Body Checkup", "Complete health checkup at any partnered hospital", "checkup", "HC", 500));
        REWARD_DEFINITIONS.add(createRewardDef("Free Blood Test", "Comprehensive blood test at a partnered lab", "health", "BT", 300));
        REWARD_DEFINITIONS.add(createRewardDef("Medicine Discount 50%", "50% discount on medicines for one month", "discount", "MD", 400));
        REWARD_DEFINITIONS.add(createRewardDef("Health Supplement Pack", "Free multivitamin and iron supplement pack", "supplement", "HS", 250));
        REWARD_DEFINITIONS.add(createRewardDef("Medicine Discount 30%", "30% discount on medicines for two weeks", "discount", "RX", 200));
        REWARD_DEFINITIONS.add(createRewardDef("Priority Hospital Access", "Priority slot booking at partnered hospitals", "priority", "PA", 250));
        REWARD_DEFINITIONS.add(createRewardDef("Health App Premium", "One month premium access to the health tracking app", "digital", "AP", 180));
        REWARD_DEFINITIONS.add(createRewardDef("First Aid Kit", "Complete first aid kit for home emergency use", "kit", "FA", 150));
        REWARD_DEFINITIONS.add(createRewardDef("Cash Reward Rs 200", "Direct cash reward", "cash", "CR", 200));
        REWARD_DEFINITIONS.add(createRewardDef("Gift Voucher Rs 500", "E-commerce gift voucher", "voucher", "GV", 300));
        REWARD_DEFINITIONS.add(createRewardDef("Food Voucher Rs 300", "Restaurant food voucher", "food", "FV", 150));
        REWARD_DEFINITIONS.add(createRewardDef("Loyalty Points x100", "100 loyalty points for future purchases", "points", "LP", 100));
        REWARD_DEFINITIONS.add(createRewardDef("VIP Donor Badge", "Exclusive badge and recognition", "badge", "BD", 350));
        REWARD_DEFINITIONS.add(createRewardDef("Donor Certificate", "Special framed certificate of appreciation", "certificate", "DC", 150));
        REWARD_DEFINITIONS.add(createRewardDef("Music Streaming 3 Months", "Free premium music streaming subscription", "entertainment", "MS", 280));
        REWARD_DEFINITIONS.add(createRewardDef("E-Book Collection", "Access to health and wellness e-books", "education", "EB", 200));
    }

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    private static Map<String, Object> createRewardDef(String title, String description, String category, String icon, int value) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("icon", icon);
        map.put("value", value);
        return map;
    }

    @GetMapping("/my-rewards")
    public ResponseEntity<?> getMyRewards(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(errorResponse("Please login first."));
        }

        try {
            List<Reward> rewards = rewardRepository.findByUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("active", rewards.stream().filter(reward -> STATUS_ACTIVE.equals(reward.getStatus())).collect(Collectors.toList()));
            response.put("redeemed", rewards.stream().filter(reward -> STATUS_REDEEMED.equals(reward.getStatus())).collect(Collectors.toList()));
            response.put("total", rewards.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse("Unable to fetch rewards right now."));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateReward(@RequestParam(required = false) Long visitId, HttpSession session) {
        return ResponseEntity.status(403)
                .body(errorResponse("Rewards are generated by admin after an approved donation visit."));
    }

    @PostMapping("/redeem/{rewardId}")
    public ResponseEntity<?> redeemReward(@PathVariable Long rewardId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(errorResponse("Please login first."));
        }

        try {
            Reward reward = rewardRepository.findById(rewardId).orElse(null);
            if (reward == null) {
                return ResponseEntity.badRequest().body(errorResponse("Reward not found."));
            }
            if (!reward.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(errorResponse("You are not authorized to redeem this reward."));
            }
            if (STATUS_REDEEMED.equals(reward.getStatus())) {
                return ResponseEntity.badRequest().body(errorResponse("This reward has already been redeemed."));
            }

            reward.setStatus(STATUS_REDEEMED);
            reward.setRedeemedDate(LocalDateTime.now());
            reward = rewardRepository.save(reward);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reward", reward);
            response.put("message", "Reward redeemed successfully. Code: " + reward.getRewardCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse("Unable to redeem this reward right now. Please try again."));
        }
    }

    @GetMapping("/definitions")
    public ResponseEntity<?> getRewardDefinitions() {
        return ResponseEntity.ok(REWARD_DEFINITIONS);
    }

    private String generateRewardCode() {
        return "RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
