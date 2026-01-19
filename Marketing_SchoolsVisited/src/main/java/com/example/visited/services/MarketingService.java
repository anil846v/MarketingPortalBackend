package com.example.visited.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.visited.entitys.SchoolVisited;
import com.example.visited.entitys.Modules;
import com.example.visited.entitys.SchoolModuleRequired;
import com.example.visited.entitys.MarketingTeam;
import com.example.visited.entitys.User;
import com.example.visited.repositories.SchoolsVisitedRepository;
import com.example.visited.repositories.ModulesRepository;
import com.example.visited.repositories.SchoolModuleRequiredRepository;
import com.example.visited.repositories.MarketingTeamRepository;
import com.example.visited.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class MarketingService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingService.class);
    private final SchoolsVisitedRepository schoolVisitedRepository;
    private final ModulesRepository modulesRepository;
    private final SchoolModuleRequiredRepository schoolModuleRequiredRepository;
    private final MarketingTeamRepository marketingTeamRepository;
    private final UserRepository userRepository;

    public MarketingService(SchoolsVisitedRepository schoolVisitedRepository,
                           ModulesRepository modulesRepository,
                           SchoolModuleRequiredRepository schoolModuleRequiredRepository,
                           MarketingTeamRepository marketingTeamRepository,
                           UserRepository userRepository) {
        this.schoolVisitedRepository = schoolVisitedRepository;
        this.modulesRepository = modulesRepository;
        this.schoolModuleRequiredRepository = schoolModuleRequiredRepository;
        this.marketingTeamRepository = marketingTeamRepository;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> getAllActiveModules() {
        List<Modules> modules = modulesRepository.findByIsActiveTrue();
        logger.debug("Found {} active modules", modules.size());
        List<Map<String, Object>> moduleList = new ArrayList<>();
        
        for (Modules module : modules) {
            Map<String, Object> moduleMap = new HashMap<>();
            moduleMap.put("id", module.getId());
            moduleMap.put("moduleName", module.getModuleName());
            moduleMap.put("description", module.getDescription());
            moduleList.add(moduleMap);
        }
        
        return moduleList;
    }

    public Map<String, Object> saveSchoolVisit(Map<String, Object> visitData, Integer userId) {
    
        User user = userRepository.findByUserId(userId);
        if (user == null || user.getRole() != User.Role.MARKETING) {
            throw new IllegalArgumentException("Marketing user not found");
        }
        
        // Get marketing team profile
        MarketingTeam team = marketingTeamRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Marketing profile not found"));
        logger.info("Saving school visit for user: {}, school: {}", userId, visitData.get("schoolName"));
        // Create SchoolVisited entity
        SchoolVisited schoolVisit = new SchoolVisited();
        
        // Set user ID
        schoolVisit.setUserId(userId);
        
        // Basic school information
        schoolVisit.setSchoolName((String) visitData.get("schoolName"));
        schoolVisit.setVisitedDate(LocalDate.now());

        schoolVisit.setMarketingExecutiveName(team.getFullName());
        schoolVisit.setLocationCity((String) visitData.get("locationCity"));
        
        // Contact information
        schoolVisit.setContactPersonName((String) visitData.get("contactPersonName"));
        schoolVisit.setDesignation((String) visitData.get("designation"));
        schoolVisit.setContactNo((String) visitData.get("contactNo"));
        schoolVisit.setEmailId((String) visitData.get("emailId"));
        
        // School details
        schoolVisit.setSchoolStrenght((Integer) visitData.get("schoolStrenght"));
        schoolVisit.setBoards((String) visitData.get("boards"));
        schoolVisit.setCurrentSystem((String) visitData.get("currentSystem"));
        schoolVisit.setNoOfUsers((Integer) visitData.get("noOfUsers"));
        
        // Requirements
        schoolVisit.setDataMigrationRequired((String) visitData.get("dataMigrationRequired"));
        schoolVisit.setCustomFeaturesRequired((String) visitData.get("customFeaturesRequired"));
        schoolVisit.setCustomFeatureDescription((String) visitData.get("customFeatureDescription"));
        schoolVisit.setRfidIntegration((String) visitData.get("rfidIntegration"));
        schoolVisit.setIdCards((String) visitData.get("idCards"));
        schoolVisit.setPaymentGatewayPreference((String) visitData.get("paymentGatewayPreference"));
        
        // Budget and timeline
        if (visitData.get("budgetRange") != null) {
            schoolVisit.setBudgetRange(new BigDecimal(visitData.get("budgetRange").toString()));
        }
        
     // Line ~81 - Expected Go-Live Date  
        String expectedGoLiveStr = (String) visitData.get("expected_goLive_date");
        if (expectedGoLiveStr != null && !expectedGoLiveStr.trim().isEmpty()) {
        	// New (after entity rename)
        	schoolVisit.setExpectedGoLiveDate(LocalDate.parse(expectedGoLiveStr));        }
        
        // Decision making 
        schoolVisit.setDecisionMakerName((String) visitData.get("decisionMakerName"));
        schoolVisit.setDecisionTimeline((String) visitData.get("decisionTimeline"));
        
        // Demo and proposal
        schoolVisit.setDemoRequired((String) visitData.get("demoRequired"));
        String demoDateStr = (String) visitData.get("demoDate");
        if (demoDateStr != null && !demoDateStr.trim().isEmpty()) {
            schoolVisit.setDemoDate(LocalDate.parse(demoDateStr));
        }
        schoolVisit.setProposalSent((String) visitData.get("proposalSent"));
        String proposalDateStr = (String) visitData.get("proposalDate");
        if (proposalDateStr != null && !proposalDateStr.trim().isEmpty()) {
            schoolVisit.setProposalDate(LocalDate.parse(proposalDateStr));
        }
        
        schoolVisit.setStatus(SchoolVisited.VisitStatus.PENDING);
        
        // Save school visit
        SchoolVisited savedSchoolVisit = schoolVisitedRepository.save(schoolVisit);
        logger.info("School visit saved with ID: {}", savedSchoolVisit.getId());
        
        // Handle selected modules
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selectedModules = (List<Map<String, Object>>) visitData.get("selectedModules");
        
        if (selectedModules != null) {
            for (Map<String, Object> moduleData : selectedModules) {
                SchoolModuleRequired schoolModule = new SchoolModuleRequired();
                schoolModule.setSchoolVisitedId(savedSchoolVisit.getId());
                schoolModule.setModuleId((Integer) moduleData.get("moduleId"));
                schoolModule.setIsSelected(SchoolModuleRequired.IsSelected.valueOf((String) moduleData.get("isSelected")));
                schoolModule.setRemarks((String) moduleData.get("remarks"));
                
                schoolModuleRequiredRepository.save(schoolModule);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "School visit saved successfully");
        response.put("schoolVisitId", savedSchoolVisit.getId());
        response.put("status", "success");
        
        return response;
    }

    public Map<String, Object> updateSchoolVisit(Integer visitId, Map<String, Object> visitData, Integer userId) {
        SchoolVisited visit = schoolVisitedRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("School visit not found"));
        
        // Verify ownership
        if (!visit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only edit your own school visits");
        }
        
        // Prevent editing restricted fields
        if (visitData.containsKey("schoolName") || visitData.containsKey("visitedDate") || visitData.containsKey("locationCity")) {
            throw new IllegalArgumentException("Cannot edit schoolName, visitedDate, or locationCity");
        }
        
//        // Update allowed fields
//        if (visitData.containsKey("marketingExecutiveName")) {
//            visit.setMarketingExecutiveName((String) visitData.get("marketingExecutiveName"));
//        }
        if (visitData.containsKey("contactPersonName")) {
            visit.setContactPersonName((String) visitData.get("contactPersonName"));
        }
        if (visitData.containsKey("designation")) {
            visit.setDesignation((String) visitData.get("designation"));
        }
        if (visitData.containsKey("contactNo")) {
            visit.setContactNo((String) visitData.get("contactNo"));
        }
        if (visitData.containsKey("emailId")) {
            visit.setEmailId((String) visitData.get("emailId"));
        }
        if (visitData.containsKey("schoolStrenght")) {
            visit.setSchoolStrenght((Integer) visitData.get("schoolStrenght"));
        }
        if (visitData.containsKey("boards")) {
            visit.setBoards((String) visitData.get("boards"));
        }
        if (visitData.containsKey("currentSystem")) {
            visit.setCurrentSystem((String) visitData.get("currentSystem"));
        }
        if (visitData.containsKey("noOfUsers")) {
            visit.setNoOfUsers((Integer) visitData.get("noOfUsers"));
        }
        if (visitData.containsKey("dataMigrationRequired")) {
            visit.setDataMigrationRequired((String) visitData.get("dataMigrationRequired"));
        }
        if (visitData.containsKey("customFeaturesRequired")) {
            visit.setCustomFeaturesRequired((String) visitData.get("customFeaturesRequired"));
        }
        if (visitData.containsKey("customFeatureDescription")) {
            visit.setCustomFeaturesRequired((String) visitData.get("customFeatureDescription"));
        }
        if (visitData.containsKey("rfidIntegration")) {
            visit.setRfidIntegration((String) visitData.get("rfidIntegration"));
        }
        if (visitData.containsKey("idCards")) {
            visit.setIdCards((String) visitData.get("idCards"));
        }
        if (visitData.containsKey("paymentGatewayPreference")) {
            visit.setPaymentGatewayPreference((String) visitData.get("paymentGatewayPreference"));
        }
        if (visitData.containsKey("budgetRange") && visitData.get("budgetRange") != null) {
            visit.setBudgetRange(new BigDecimal(visitData.get("budgetRange").toString()));
        }
        if (visitData.containsKey("expectedGoLiveDate") && visitData.get("expectedGoLiveDate") != null) {
            visit.setExpectedGoLiveDate(LocalDate.parse((String) visitData.get("expectedGoLiveDate")));
        }
        if (visitData.containsKey("decisionMakerName")) {
            visit.setDecisionMakerName((String) visitData.get("decisionMakerName"));
        }
        if (visitData.containsKey("decisionTimeline")) {
            visit.setDecisionTimeline((String) visitData.get("decisionTimeline"));
        }
        if (visitData.containsKey("demoRequired")) {
            visit.setDemoRequired((String) visitData.get("demoRequired"));
        }
        if (visitData.containsKey("demoDate") && visitData.get("demoDate") != null) {
            visit.setDemoDate(LocalDate.parse((String) visitData.get("demoDate")));
        }
        if (visitData.containsKey("proposalSent")) {
            visit.setProposalSent((String) visitData.get("proposalSent"));
        }
        if (visitData.containsKey("proposalDate") && visitData.get("proposalDate") != null) {
            visit.setProposalDate(LocalDate.parse((String) visitData.get("proposalDate")));
        }
        
        schoolVisitedRepository.save(visit);
        logger.info("School visit updated: {}", visitId);
        
        return Map.of(
            "message", "School visit updated successfully",
            "visitId", visitId,
            "status", "success"
        );
    }

    public List<Map<String, Object>> getAllSchoolVisits(Integer userId) {
        List<SchoolVisited> visits = schoolVisitedRepository.findByUserId(userId);
        logger.debug("Retrieved {} school visits for user: {}", visits.size(), userId);
        List<Map<String, Object>> visitList = new ArrayList<>();
        
        for (SchoolVisited visit : visits) {
            Map<String, Object> visitMap = new HashMap<>();
            visitMap.put("id", visit.getId());
            visitMap.put("schoolName", visit.getSchoolName());
            visitMap.put("visitedDate", visit.getVisitedDate());
            visitMap.put("marketingExecutiveName", visit.getMarketingExecutiveName());
            visitMap.put("locationCity", visit.getLocationCity());
            visitMap.put("contactPersonName", visit.getContactPersonName());
            visitMap.put("decisionMakerName", visit.getDecisionMakerName());
            visitMap.put("decisionTimeline", visit.getDecisionTimeline());
            visitMap.put("contactNo", visit.getContactNo());
            visitMap.put("emailId", visit.getEmailId());
            visitMap.put("schoolStrenght", visit.getSchoolStrenght());
            visitMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
            
            // Only show payment fields if status is ACCEPTED
            if (visit.getStatus() == SchoolVisited.VisitStatus.ACCEPTED) {
                visitMap.put("orderBookingDate", visit.getOrderBookingDate());
                visitMap.put("initialPayment", visit.getInitialPayment());
                visitMap.put("paymentTerms", visit.getPaymentTerms());
                
            }
            
            visitMap.put("currentSystem", visit.getCurrentSystem());
            visitMap.put("noOfUsers", visit.getNoOfUsers());
            visitMap.put("dataMigrationRequired", visit.getDataMigrationRequired());
            visitMap.put("customFeaturesRequired", visit.getCustomFeaturesRequired());
            visitMap.put("customFeatureDescription", visit.getCustomFeatureDescription());
            visitMap.put("rfidIntegration", visit.getRfidIntegration());
            visitMap.put("idCards", visit.getIdCards());
            visitMap.put("paymentGatewayPreference", visit.getPaymentGatewayPreference());
            visitMap.put("budgetRange", visit.getBudgetRange());
            visitMap.put("costPerMember", visit.getCostPerMember());
            visitMap.put("demoRequired", visit.getDemoRequired());
            visitMap.put("demoDate", visit.getDemoDate());
            visitMap.put("proposalSent", visit.getProposalSent());
            visitMap.put("proposalDate", visit.getProposalDate());
            visitMap.put("status", visit.getStatus());
            if (visit.getStatus() == SchoolVisited.VisitStatus.REJECTED) {
                visitMap.put("rejectionReason", visit.getRejectionReason());
            }
            visitMap.put("createdAt", visit.getCreatedAt());
            
            // Get selected modules for this visit
            List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisitedId(visit.getId());
            List<Map<String, Object>> moduleList = new ArrayList<>();
            
            for (SchoolModuleRequired module : modules) {
                Map<String, Object> moduleMap = new HashMap<>();
                moduleMap.put("moduleId", module.getModuleId());
                moduleMap.put("isSelected", module.getIsSelected().name());
                moduleMap.put("remarks", module.getRemarks());
                moduleList.add(moduleMap);
            }
            
            visitMap.put("selectedModules", moduleList);
            visitList.add(visitMap);
        }
        
        return visitList;
    }

    public List<Map<String, Object>> getAllSchoolVisitsForAdmin() {
        List<SchoolVisited> visits = schoolVisitedRepository.findAll();
        logger.debug("Retrieved {} school visits for admin", visits.size());
        List<Map<String, Object>> visitList = new ArrayList<>();
        
        for (SchoolVisited visit : visits) {
            Map<String, Object> visitMap = new HashMap<>();
            visitMap.put("id", visit.getId());
            visitMap.put("schoolName", visit.getSchoolName());
            visitMap.put("visitedDate", visit.getVisitedDate());
            visitMap.put("marketingExecutiveName", visit.getMarketingExecutiveName());
            visitMap.put("locationCity", visit.getLocationCity());
            visitMap.put("contactPersonName", visit.getContactPersonName());
            visitMap.put("contactNo", visit.getContactNo());
            visitMap.put("emailId", visit.getEmailId());
            visitMap.put("schoolStrenght", visit.getSchoolStrenght());
            visitMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
            visitMap.put("orderBookingDate", visit.getOrderBookingDate());
            visitMap.put("initialPayment", visit.getInitialPayment());
            visitMap.put("paymentTerms", visit.getPaymentTerms());
            visitMap.put("costPerMember", visit.getCostPerMember());
            visitMap.put("CurrentSystem", visit.getCurrentSystem());
            visitMap.put("No Of Users ", visit.getNoOfUsers());
            visitMap.put("Data Migration Required", visit.getDataMigrationRequired());
            visitMap.put("Custom Features Required", visit.getCustomFeaturesRequired());
            visitMap.put("Custom Features Description", visit.getCustomFeatureDescription());
            visitMap.put("RFID Integration", visit.getRfidIntegration());
            visitMap.put("Id Cards", visit.getIdCards());
            visitMap.put("Payment GateWay Preference", visit.getPaymentGatewayPreference());
            visitMap.put("Budget Range", visit.getBudgetRange());
            visitMap.put("Demo Required", visit.getDemoRequired());
            visitMap.put("Demo Date", visit.getDemoDate());
            visitMap.put("Proposal Sent", visit.getProposalSent());
            visitMap.put("Proposal date", visit.getProposalDate());
            visitMap.put("status", visit.getStatus());
            if (visit.getStatus() == SchoolVisited.VisitStatus.REJECTED) {
                visitMap.put("rejectionReason", visit.getRejectionReason());
            }
            visitMap.put("createdAt", visit.getCreatedAt());
            
            // Get selected modules for this visit
            List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisitedId(visit.getId());
            List<Map<String, Object>> moduleList = new ArrayList<>();
            
            for (SchoolModuleRequired module : modules) {
                Map<String, Object> moduleMap = new HashMap<>();
                moduleMap.put("moduleId", module.getModuleId());
                moduleMap.put("isSelected", module.getIsSelected().name());
                moduleMap.put("remarks", module.getRemarks());
                moduleList.add(moduleMap);
            }
            
            visitMap.put("selectedModules", moduleList);
            visitList.add(visitMap);
        }
        
        return visitList;
    }

    public List<Map<String, Object>> getAcceptedOrders() {
        List<SchoolVisited> acceptedVisits = schoolVisitedRepository.findByStatus(SchoolVisited.VisitStatus.ACCEPTED);
        logger.info("Retrieved {} accepted orders", acceptedVisits.size());
        List<Map<String, Object>> orderList = new ArrayList<>();
        
        for (SchoolVisited visit : acceptedVisits) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", visit.getId());
            orderMap.put("schoolName", visit.getSchoolName());
            orderMap.put("orderBookingDate", visit.getOrderBookingDate());
            orderMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
            orderMap.put("schoolStrenght", visit.getSchoolStrenght());
            orderMap.put("Budget Range", visit.getBudgetRange());
            orderMap.put("initialPayment", visit.getInitialPayment());
            orderMap.put("paymentTerms", visit.getPaymentTerms());
            orderMap.put("costPerMember", visit.getCostPerMember());
            orderMap.put("currentSystem", visit.getCurrentSystem());
            orderMap.put("idCards", visit.getIdCards());
            orderMap.put("RFID Integration", visit.getRfidIntegration());
            orderMap.put("Payment GateWay Preference", visit.getPaymentGatewayPreference());
            
            orderMap.put("marketingExecutiveName", visit.getMarketingExecutiveName());
            orderMap.put("locationCity", visit.getLocationCity());
            orderMap.put("contactPersonName", visit.getContactPersonName());
            orderMap.put("designation", visit.getDesignation());
            orderMap.put("contactNo", visit.getContactNo());
            orderMap.put("emailId", visit.getEmailId());
            orderMap.put("proposalSent", visit.getProposalSent());
            orderMap.put("status", visit.getStatus().name());
            orderMap.put("createdAt", visit.getCreatedAt());
            
            // Get selected modules
            List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisitedId(visit.getId());
            List<Map<String, Object>> moduleList = new ArrayList<>();
            
            for (SchoolModuleRequired module : modules) {
                Map<String, Object> moduleMap = new HashMap<>();
                moduleMap.put("moduleId", module.getModuleId());
                moduleMap.put("isSelected", module.getIsSelected().name());
                moduleMap.put("remarks", module.getRemarks());
                moduleList.add(moduleMap);
            }
            
            orderMap.put("selectedModules", moduleList);
            orderList.add(orderMap);
        }
        
        return orderList;
    }

    public Map<String, Object> changeVisitStatus(Integer visitId, String newStatus, Map<String, Object> statusData) {
        SchoolVisited visit = schoolVisitedRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("School visit not found"));
        
        SchoolVisited.VisitStatus status = SchoolVisited.VisitStatus.valueOf(newStatus.toUpperCase());
        
        // If status is REJECTED, rejection reason is required
        if (status == SchoolVisited.VisitStatus.REJECTED) {
            String rejectionReason = (String) statusData.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a visit");
            }
            visit.setRejectionReason(rejectionReason.trim());
            logger.info("Visit rejected with reason: {}", rejectionReason);
        }
        
        visit.setStatus(status);
        
        // If status changed to ACCEPTED, set orderBookingDate automatically
        if (status == SchoolVisited.VisitStatus.ACCEPTED) {
            visit.setOrderBookingDate(LocalDate.now());
            
            // Update payment fields if provided
            if (statusData.containsKey("initialPayment")) {
                visit.setInitialPayment(new BigDecimal(statusData.get("initialPayment").toString()));
            }
            if (statusData.containsKey("paymentTerms")) {
                visit.setPaymentTerms((String) statusData.get("paymentTerms"));
            }
            if (statusData.containsKey("costPerMember")) {
                visit.setCostPerMember(new BigDecimal(statusData.get("costPerMember").toString()));
            }
            
            logger.info("Order booking date set to {} for visit ID: {}", LocalDate.now(), visitId);
        }
        
        schoolVisitedRepository.save(visit);
        logger.info("Visit status changed to {} for visit ID: {}", status, visitId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Status updated successfully");
        response.put("visitId", visitId);
        response.put("newStatus", status.name());
        if (visit.getOrderBookingDate() != null) {
            response.put("orderBookingDate", visit.getOrderBookingDate().toString());
        }
        return response;
    }

    public void deleteRejectedSchoolVisit(Integer visitId) {
        SchoolVisited visit = schoolVisitedRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("School visit not found"));
        
        // Only allow deletion of rejected visits
        if (visit.getStatus() != SchoolVisited.VisitStatus.REJECTED) {
            throw new IllegalArgumentException("Only rejected school visits can be deleted");
        }
        
        // Delete related modules first
        List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisitedId(visitId);
        schoolModuleRequiredRepository.deleteAll(modules);
        
        // Delete the school visit
        schoolVisitedRepository.delete(visit);
        logger.info("Rejected school visit deleted: {}", visitId);
    }

    public Map<String, Object> getMarketingProfileInfo(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        // Get user info
        User user = userRepository.findByUserId(userId);
        if (user == null || user.getRole() != User.Role.MARKETING) {
            throw new IllegalArgumentException("Marketing user not found");
        }
        
        // Get marketing team profile
        MarketingTeam team = marketingTeamRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Marketing profile not found"));
        
        // Build response with only necessary fields
        Map<String, Object> profile = new HashMap<>();
        
        // Personal info
        profile.put("userId", user.getUserId());
        profile.put("profilePhotoPath", team.getProfilePhotoPath());
        profile.put("fullName", team.getFullName());
        profile.put("email", team.getEmail());
        profile.put("phoneNumber", team.getPhoneNumber());
        profile.put("age", team.getAge());
        profile.put("gender", team.getGender() != null ? team.getGender().name() : null);
        profile.put("address", team.getAddress());
        
        // Work info
        profile.put("designation", team.getDesignation());
        profile.put("assignedRegion", team.getAssignedRegion());
        profile.put("targetDistricts", team.getTargetDistricts());
        
        // Account info
        profile.put("role", user.getRole().name());
        profile.put("status", user.getStatus().name());
        profile.put("memberSince", user.getCreatedAt());
        
//        // Activity stats (efficient count queries)
//        long totalVisits = schoolVisitedRepository.countByUserId(userId);
//        long pendingVisits = schoolVisitedRepository.countByUserIdAndStatus(userId, SchoolVisited.VisitStatus.PENDING);
//        long acceptedVisits = schoolVisitedRepository.countByUserIdAndStatus(userId, SchoolVisited.VisitStatus.ACCEPTED);
//        long rejectedVisits = schoolVisitedRepository.countByUserIdAndStatus(userId, SchoolVisited.VisitStatus.REJECTED);
//        
//        profile.put("totalVisits", totalVisits);
//        profile.put("pendingVisits", pendingVisits);
//        profile.put("acceptedVisits", acceptedVisits);
//        profile.put("rejectedVisits", rejectedVisits);
        
        logger.debug("Profile fetched for user: {}", userId);
        return profile;
    }
}