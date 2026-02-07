package com.example.visited.controllers;

import com.example.visited.entitys.Modules;
import org.springframework.http.MediaType; // For MULTIPART_FORM_DATA_VALUE
import org.springframework.web.multipart.MultipartFile; // For @RequestPart
import com.example.visited.entitys.User;
import com.example.visited.services.AdmiService;
import com.example.visited.services.MarketingService;
import com.example.visited.services.CommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${cors.allowed-origin:http://localhost:5173}", allowCredentials = "true")
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdmiService admiService;
    private final MarketingService marketingService;
    private final CommunicationService communicationService;

    public AdminController(AdmiService admiService, MarketingService marketingService, CommunicationService communicationService) {
        this.admiService = admiService;
        this.marketingService = marketingService;
        this.communicationService = communicationService;
    }

  

    @PostMapping(value = "/register-marketing-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerMarketingUser(
            @RequestPart("userData") Map<String, Object> userData,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            HttpServletRequest request) {

        try {
            User currentUser = (User) request.getAttribute("authenticatedUser");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            if (currentUser.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Marketing role required"));
            }

            if (photo != null && !photo.isEmpty()) {
                userData.put("photo", photo);
            }

            User newUser = admiService.registerMarketingUser(userData);  // ← fixed typo: admiService → adminService

            return ResponseEntity.ok(Map.of(
                    "message", newUser.getUsername() + " registered successfully",
                    "status", "approved"
            ));

        } catch (IllegalArgumentException e) {
            // most common from service: duplicate email/phone, invalid gender, etc.
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // everything else (DB error, file issue, unexpected NPE...)
            // log it so you can debug later
            logger.error("Failed to register marketing user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed. Try again later."));
        }
    }
    @PutMapping("/schoolVisits/{id}")
    public ResponseEntity<Map<String, Object>> updateSchoolVisitAsAdmin(
            @PathVariable("id") Integer visitId,
            @RequestBody Map<String, Object> visitData) {

        Map<String, Object> result = marketingService.adminUpdateSchoolVisit(visitId, visitData);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/school-visit")
    public ResponseEntity<?> submitSchoolVisit(@RequestBody Map<String, Object> visitData, HttpServletRequest request) {
        try {
            User user = (User) request.getAttribute("authenticatedUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            Map<String, Object> result = marketingService.saveSchoolVisit(visitData, user.getUserId());
            logger.info("School visit saved successfully: {}", result.get("schoolVisitId"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to save school visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save school visit"));
        }
    }

    @GetMapping("/marketing-users")
    public ResponseEntity<?> getAllMarketingUsers(HttpServletRequest request) {
    	User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Marketing role required"));
        }
        List<Map<String, Object>> users = admiService.getAllMarketingUsers();

        return ResponseEntity.ok(Map.of(
                "message", "Marketing users fetched successfully",
                "total", users.size(),
                "users", users
        ));
    }
    
    @GetMapping("/schoolvisits-user/{userId}")
    public ResponseEntity<?> getschoolvisitsbyUser(
            HttpServletRequest request,
            @PathVariable Integer userId) {

        User user = (User) request.getAttribute("authenticatedUser");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin role required"));
        }

        List<Map<String, Object>> data = marketingService.getAllSchoolVisits(userId);

        return ResponseEntity.ok(data);
    }


    @PutMapping(value = "/update-marketing-user/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMarketingUser(
            @PathVariable Integer userId,
            @RequestPart("userData") Map<String, Object> userData,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            HttpServletRequest request
    ) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
        }

        // Optional: Add photo to userData map so service layer can handle it
        if (photo != null && !photo.isEmpty()) {
            userData.put("photo", photo);
        }

        User updated = admiService.updateMarketingUser(userId, userData);

        return ResponseEntity.ok(Map.of(
                "message", updated.getUsername() + " updated successfully",
                "userId", updated.getUserId(),
                "status", "updated"
        ));
    }

    @PutMapping("/change-user-status/{userId}")
    public ResponseEntity<?> changeUserStatus(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> statusData,
            HttpServletRequest request) {
    	User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
        }

        String newStatus = statusData.get("status");
        User updated = admiService.updateMarketingUser(userId, Map.of("status", newStatus));

        return ResponseEntity.ok(Map.of(
                "message", "User status changed to " + newStatus,
                "userId", updated.getUserId(),
                "status", updated.getStatus().name()
        ));
    }

    @DeleteMapping("/delete-marketing-user/{userId}")
    public ResponseEntity<?> deleteMarketingUser(
            @PathVariable Integer userId,
            HttpServletRequest request) {
    	User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
        }
        admiService.deleteMarketingUser(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Marketing user with ID " + userId + " deleted successfully",
                "status", "deleted"
        ));
    }

    // Read-only endpoints - no admin check needed (filter already protected /admin/*)
    @GetMapping("/school-visits")
    public ResponseEntity<List<Map<String, Object>>> getAllSchoolVisits() {
        return ResponseEntity.ok(marketingService.getAllSchoolVisitsForAdmin());
    }

    @GetMapping("/accepted-orders")
    public ResponseEntity<List<Map<String, Object>>> getAcceptedOrders() {
        return ResponseEntity.ok(marketingService.getAcceptedOrders());
    }

    @GetMapping("/modules")
    public ResponseEntity<List<Map<String, Object>>> getAllModules() {
        List<Modules> modules = admiService.getAllModules();
        List<Map<String, Object>> moduleList = new ArrayList<>();
        for (Modules module : modules) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", module.getId());
            map.put("moduleName", module.getModuleName());
            map.put("description", module.getDescription());
            map.put("isActive", module.getIsActive());
            moduleList.add(map);
        }
        return ResponseEntity.ok(moduleList);
    }

    @PostMapping("/modules")
    public ResponseEntity<?> createModule(
            @RequestBody Map<String, Object> moduleData,
            HttpServletRequest request) {
    	User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
        }

        Modules created = admiService.createModule(moduleData);

        return ResponseEntity.ok(Map.of(
                "message", "Module '" + created.getModuleName() + "' created successfully",
                "moduleId", created.getId(),
                "status", "success"
        ));
    }

    @PutMapping("/update-modules/{moduleId}")
    public ResponseEntity<?> updateModules(
            @PathVariable Integer moduleId,
            @RequestBody Map<String, Object> moduleData,
            HttpServletRequest request) {

        try {
            User currentUser = (User) request.getAttribute("authenticatedUser");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            if (currentUser.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin role required"));
            }

            Modules updated = admiService.updateModule(moduleId, moduleData);  // ← fixed typo: admiService → adminService

            return ResponseEntity.ok(Map.of(
                    "message", "Module '" + updated.getModuleName() + "' updated successfully",
                    "moduleId", updated.getId(),
                    "status", "success"
            ));

        } catch (IllegalArgumentException e) {
            // Covers: module not found, empty name, wrong isActive type, etc.
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // Database failure, unexpected NPE, etc.
            logger.error("Failed to update module id: {}", moduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update module. Please try again."));
        }
    }


    // COMMUNICATION ENDPOINTS
    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(
            @RequestBody Map<String, String> data,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (data.get("title") == null || data.get("message") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title and message are required"));
            }
            return ResponseEntity.ok(communicationService.createAnnouncement(
                data.get("title"), data.get("message"), admin.getUserId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create announcement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create announcement"));
        }
    }

    @GetMapping("/announcements")
    public ResponseEntity<?> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (admin.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
            }
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            return ResponseEntity.ok(communicationService.getAllAnnouncementsPaginated(pageable));
        } catch (Exception e) {
            logger.error("Failed to fetch announcements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch announcements"));
        }
    }

    @PostMapping("/messages/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (data.get("receiverId") == null || data.get("message") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "receiverId and message are required"));
            }
            Integer receiverId = data.get("receiverId") instanceof Integer ? 
                (Integer) data.get("receiverId") : Integer.parseInt(data.get("receiverId").toString());
            return ResponseEntity.ok(communicationService.sendMessage(
                admin.getUserId(), receiverId, (String) data.get("message")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to send message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send message"));
        }
    }

    @GetMapping("/messages/conversation/{userId}")
    public ResponseEntity<?> getConversation(@PathVariable Integer userId, HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            communicationService.markConversationAsRead(admin.getUserId(), userId);
            return ResponseEntity.ok(communicationService.getConversation(admin.getUserId(), userId));
        } catch (Exception e) {
            logger.error("Failed to fetch conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch conversation"));
        }
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Integer messageId,
            @RequestParam(defaultValue = "FOR_ME") String deleteType,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            communicationService.deleteMessage(messageId, admin.getUserId(), deleteType);
            return ResponseEntity.ok(Map.of("message", "Message deleted"));
        } catch (Exception e) {
            logger.error("Failed to delete message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete message"));
        }
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Integer announcementId, HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            communicationService.deleteAnnouncement(announcementId, admin.getUserId());
            return ResponseEntity.ok(Map.of("message", "Announcement deleted"));
        } catch (Exception e) {
            logger.error("Failed to delete announcement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete announcement"));
        }
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            return ResponseEntity.ok(Map.of("count", communicationService.getUnreadCount(admin.getUserId())));
        } catch (Exception e) {
            logger.error("Failed to get unread count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread count"));
        }
    }

    @GetMapping("/announcements/{announcementId}/read-status")
    public ResponseEntity<?> getAnnouncementReadStatus(@PathVariable Integer announcementId) {
        try {
            return ResponseEntity.ok(communicationService.getAnnouncementReadStatus(announcementId));
        } catch (Exception e) {
            logger.error("Failed to get announcement read status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get read status"));
        }
    }

    @DeleteMapping("/school-visits/{visitId}")
    public ResponseEntity<?> deleteSchoolVisit(
            @PathVariable Integer visitId,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (admin.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
            }
            
            marketingService.deleteschoolVisit(visitId);
            return ResponseEntity.ok(Map.of("message", " school visit deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete school visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete school visit"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getAdminProfile(HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (admin.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
            }
            
            Map<String, Object> profile = admiService.getAdminProfile(admin.getUserId());
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch admin profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateAdminProfile(
            @RequestBody Map<String, Object> profileData,
            HttpServletRequest request) {
        try {
            User admin = (User) request.getAttribute("authenticatedUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            if (admin.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin role required"));
            }
            
            // Input sanitization
            if (profileData.containsKey("username")) {
                String username = (String) profileData.get("username");
                if (username != null) {
                    profileData.put("username", username.trim().toLowerCase());
                }
            }
            
            Map<String, Object> result = admiService.updateAdminProfile(admin.getUserId(), profileData);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update admin profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile"));
        }
    }
}