package com.example.visited.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.visited.services.MarketingService;
import com.example.visited.services.CommunicationService;
import com.example.visited.services.AdminLookupService;
import com.example.visited.entitys.User;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = "${cors.allowed-origin:http://localhost:5173}", allowCredentials = "true")
@RequestMapping("/api/marketing")
public class MarketingController {

    private static final Logger logger = LoggerFactory.getLogger(MarketingController.class);
    private final MarketingService marketingService;
    private final CommunicationService communicationService;
    private final AdminLookupService adminLookupService;

    public MarketingController(MarketingService marketingService, CommunicationService communicationService,
                               AdminLookupService adminLookupService) {
        this.marketingService = marketingService;
        this.communicationService = communicationService;
        this.adminLookupService = adminLookupService;
    }

    

    @GetMapping("/modules")
    public ResponseEntity<?> getAllModules(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            List<Map<String, Object>> modules = marketingService.getAllActiveModules();
            logger.info("Fetched {} active modules", modules.size());
            return ResponseEntity.ok(modules);
        } catch (Exception e) {
            logger.error("Failed to fetch modules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch modules"));
        }
    }
    

    @PostMapping("/school-visit")
    public ResponseEntity<?> submitSchoolVisit(@RequestBody Map<String, Object> visitData, HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
//            if (user.getRole() != User.Role.MARKETING) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
//            }
            Map<String, Object> result = marketingService.saveSchoolVisit(visitData, user.getUserId());
            logger.info("School visit saved successfully: {}", result.get("schoolVisitId"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to save school visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save school visit"));
        }
    }
    
    

    @PutMapping("/school-visit/{visitId}")
    public ResponseEntity<?> updateSchoolVisit(
            @PathVariable Integer visitId,
            @RequestBody Map<String, Object> visitData,
            HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            Map<String, Object> result = marketingService.updateSchoolVisit(visitId, visitData, user.getUserId());
            logger.info("School visit updated successfully: {}", visitId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update school visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update school visit"));
        }
    }

    @GetMapping("/school-visits")
    public ResponseEntity<?> getAllSchoolVisits(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            List<Map<String, Object>> visits = marketingService.getAllSchoolVisits(user.getUserId());
            logger.info("Fetched {} school visits for user: {}", visits.size(), user.getUserId());
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            logger.error("Failed to fetch school visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch school visits"));
        }
    }

    @PutMapping("/change-visit-status/{visitId}")
    public ResponseEntity<?> changeVisitStatus(
            @PathVariable Integer visitId,
            @RequestBody Map<String, Object> statusData,
            HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            String newStatus = (String) statusData.get("status");
            Map<String, Object> result = marketingService.changeVisitStatus(visitId, newStatus, statusData);
            logger.info("Visit status changed for ID: {}", visitId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to change visit status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change status"));
        }
    }

    // COMMUNICATION ENDPOINTS
    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            List<Map<String, Object>> announcements = communicationService.getAllAnnouncements();
            for (Map<String, Object> announcement : announcements) {
                communicationService.markAnnouncementAsRead((Integer) announcement.get("id"), user.getUserId());
            }
            return ResponseEntity.ok(announcements);
        } catch (Exception e) {
            logger.error("Failed to fetch announcements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch announcements"));
        }
    }

    @PostMapping("/messages/send")
    public ResponseEntity<?> sendMessageToAdmin(
            @RequestBody Map<String, String> data,
            HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            if (data.get("message") == null || data.get("message").trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
            }
            Integer adminId = adminLookupService.getAdminUserId();
            return ResponseEntity.ok(communicationService.sendMessage(
                user.getUserId(), adminId, data.get("message")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send message"));
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessagesWithAdmin(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            Integer adminId = adminLookupService.getAdminUserId();
            communicationService.markConversationAsRead(user.getUserId(), adminId);
            return ResponseEntity.ok(communicationService.getConversation(user.getUserId(), adminId));
        } catch (Exception e) {
            logger.error("Failed to fetch messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch messages"));
        }
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            return ResponseEntity.ok(Map.of("unreadCount", communicationService.getUnreadCount(user.getUserId())));
        } catch (Exception e) {
            logger.error("Failed to get unread count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread count"));
        }
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Integer messageId,
            @RequestParam(defaultValue = "FOR_ME") String deleteType,
            HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            communicationService.deleteMessage(messageId, user.getUserId(), deleteType);
            return ResponseEntity.ok(Map.of("message", "Message deleted"));
        } catch (Exception e) {
            logger.error("Failed to delete message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete message"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (user.getRole() != User.Role.MARKETING) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
            }
            
            Map<String, Object> profile = marketingService.getMarketingProfileInfo(user.getUserId());
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }
}