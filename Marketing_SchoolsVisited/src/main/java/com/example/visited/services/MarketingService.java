package com.example.visited.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.visited.entitys.MarketingTeam;
import com.example.visited.entitys.Modules;
import com.example.visited.entitys.SchoolModuleRequired;
import com.example.visited.entitys.SchoolVisited;
import com.example.visited.entitys.User;
import com.example.visited.repositories.MarketingTeamRepository;
import com.example.visited.repositories.ModulesRepository;
import com.example.visited.repositories.SchoolModuleRequiredRepository;
import com.example.visited.repositories.SchoolsVisitedRepository;
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

	public MarketingService(SchoolsVisitedRepository schoolVisitedRepository, ModulesRepository modulesRepository,
			SchoolModuleRequiredRepository schoolModuleRequiredRepository,
			MarketingTeamRepository marketingTeamRepository, UserRepository userRepository) {
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
		if (user == null || (user.getRole() != User.Role.MARKETING && user.getRole() != User.Role.ADMIN)) {
			throw new IllegalArgumentException("Only Marketing or ADMIN role can save school visits");
		}

		// Get marketing team profile
		MarketingTeam team = marketingTeamRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("Marketing profile not found for user"));

		logger.info("Saving school visit for user: {}, school: {}", userId, visitData.get("schoolName"));

		SchoolVisited schoolVisit = new SchoolVisited();

		// Set relationship
		schoolVisit.setUser(user);

		// Basic school information
		schoolVisit.setSchoolName((String) visitData.get("schoolName"));
		schoolVisit.setVisitedDate(LocalDate.now());

		if (user.getRole() == User.Role.ADMIN) {
			schoolVisit.setMarketingExecutiveName("ADMIN");
		} else {
			schoolVisit.setMarketingExecutiveName(team.getFullName());
		}

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
		schoolVisit.setRequiredplatform((String) visitData.get("requiredplatform"));
		schoolVisit.setNoOfUsers((Integer) visitData.get("noOfUsers"));

		// Requirements
		schoolVisit.setDataMigrationRequired((String) visitData.get("dataMigrationRequired"));
		schoolVisit.setCustomFeaturesRequired((String) visitData.get("customFeaturesRequired"));
		schoolVisit.setCustomFeatureDescription((String) visitData.get("customFeatureDescription"));
		schoolVisit.setRfidIntegration((String) visitData.get("rfidIntegration"));
		schoolVisit.setIdCards((String) visitData.get("idCards"));
		schoolVisit.setPaymentGatewayPreference((String) visitData.get("paymentGatewayPreference"));

		// Billing & Pricing Details
		schoolVisit.setBillingfrequency((String) visitData.get("billingFrequency")); // "Monthly", "Quarterly",
																						// "HalfYearly", "Yearly"
		
		

		schoolVisit.setCostPerMember(toBigDecimall(visitData.get("costPerMember")));
		schoolVisit.setBudgetRange(toBigDecimall(visitData.get("budgetRange")));

		// Expected Go-Live Date
		String expectedGoLiveStr = (String) visitData.get("expectedGoLiveDate");
		if (expectedGoLiveStr != null && !expectedGoLiveStr.trim().isEmpty()) {
			schoolVisit.setExpectedGoLiveDate(LocalDate.parse(expectedGoLiveStr));
		}

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
				Integer moduleId = (Integer) moduleData.get("moduleId");

				// Find existing record by school and module
				Optional<SchoolModuleRequired> existingModuleOpt = schoolModuleRequiredRepository
						.findBySchoolVisitedIdAndModuleId(savedSchoolVisit.getId(), moduleId);

				SchoolModuleRequired schoolModule = existingModuleOpt.orElse(new SchoolModuleRequired());

				schoolModule.setSchoolVisited(savedSchoolVisit);
				schoolModule.setModuleId(moduleId);
				schoolModule
						.setIsSelected(SchoolModuleRequired.IsSelected.valueOf((String) moduleData.get("isSelected")));
				schoolModule.setRemarks((String) moduleData.get("remarks"));

				schoolModuleRequiredRepository.save(schoolModule);
			}
		}

		// Return success response
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("schoolVisitId", savedSchoolVisit.getId());
		response.put("message", "School visit saved successfully");

		return response;
	}
	private BigDecimal toBigDecimall(Object value) {
	    if (value == null) return null;
	    try {
	        return new BigDecimal(value.toString().trim());
	    } catch (NumberFormatException e) {
	        throw new IllegalArgumentException("Invalid number format: " + value);
	    }
	}

	public Map<String, Object> updateSchoolVisit(Integer visitId, Map<String, Object> visitData, Integer userId) {
		SchoolVisited visit = schoolVisitedRepository.findById(visitId)
				.orElseThrow(() -> new IllegalArgumentException("School visit not found"));

		// Verify ownership
		if (!visit.getUser().getUserId().equals(userId)) {
			throw new IllegalArgumentException("You can only edit your own school visits");
		}

		// Prevent editing restricted fields
		if (visitData.containsKey("schoolName") || visitData.containsKey("visitedDate")
				|| visitData.containsKey("locationCity")) {
			throw new IllegalArgumentException("Cannot edit schoolName, visitedDate, or locationCity");
		}

//        // Update allowed fields
//        if (visitData.containsKey("marketingExecutiveName")) {
//            visit.setMarketingExecutiveName((String) visitData.get("marketingExecutiveName"));
//        }
		if (visitData.containsKey("contactPersonName")) {
			visit.setContactPersonName((String) visitData.get("contactPersonName"));
		}
		if (visitData.containsKey("costPerMember")) {
			visit.setCostPerMember(new BigDecimal(visitData.get("costPerMember").toString()));
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
		if (visitData.containsKey("billingFrequency")) {
			visit.setBillingfrequency((String) visitData.get("billingFrequency"));
		}
		if (visitData.containsKey("currentSystem")) {
			visit.setCurrentSystem((String) visitData.get("currentSystem"));
		}
		if (visitData.containsKey("requiredplatform")) {
			visit.setRequiredplatform((String) visitData.get("requiredplatform"));
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
			visit.setCustomFeatureDescription((String) visitData.get("customFeatureDescription"));
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

		if (visitData.containsKey("selectedModules")) {

			List<Map<String, Object>> modules = (List<Map<String, Object>>) visitData.get("selectedModules");

			// delete old modules
			schoolModuleRequiredRepository.deleteBySchoolVisited(visit);

			// insert new modules
			for (Map<String, Object> moduleData : modules) {

				SchoolModuleRequired schoolModule = new SchoolModuleRequired();
				schoolModule.setSchoolVisited(visit); // relationship

				// Safe moduleId conversion
				Object moduleIdObj = moduleData.get("moduleId");
				if (moduleIdObj != null) {
					if (moduleIdObj instanceof Number) {
						schoolModule.setModuleId(((Number) moduleIdObj).intValue());
					} else {
						schoolModule.setModuleId(Integer.parseInt(moduleIdObj.toString()));
					}
				}

				// Safe isSelected conversion
				Object isSelectedObj = moduleData.get("isSelected");
				if (isSelectedObj != null) {
					SchoolModuleRequired.IsSelected isSelected;
					if (isSelectedObj instanceof Number) {
						int val = ((Number) isSelectedObj).intValue();
						isSelected = val == 1 ? SchoolModuleRequired.IsSelected.Yes
								: SchoolModuleRequired.IsSelected.No;
					} else {
						isSelected = SchoolModuleRequired.IsSelected.valueOf(isSelectedObj.toString());
					}
					schoolModule.setIsSelected(isSelected);
				}

				schoolModule.setRemarks((String) moduleData.get("remarks"));

				schoolModuleRequiredRepository.save(schoolModule);
			}
		}

		schoolVisitedRepository.save(visit);
		logger.info("School visit updated: {}", visitId);

		return Map.of("message", "School visit updated successfully", "visitId", visitId, "status", "success");
	}

	public List<Map<String, Object>> getAllSchoolVisits(Integer userId) {
		User user = userRepository.findByUserId(userId);
		List<SchoolVisited> visits = schoolVisitedRepository.findByUser(user);
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
			visitMap.put("designation", visit.getDesignation());
			visitMap.put("decisionMakerName", visit.getDecisionMakerName());
			visitMap.put("boards", visit.getBoards());
			visitMap.put("decisionTimeline", visit.getDecisionTimeline());
			visitMap.put("contactNo", visit.getContactNo());
			visitMap.put("emailId", visit.getEmailId());
			visitMap.put("schoolStrenght", visit.getSchoolStrenght());
			visitMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
			visitMap.put("billingFrequency",visit.getBillingfrequency()); // "Monthly", "Quarterly",


			// Only show payment fields if status is ACCEPTED
			if (visit.getStatus() == SchoolVisited.VisitStatus.ACCEPTED) {
				visitMap.put("orderBookingDate", visit.getOrderBookingDate());
				visitMap.put("initialPayment", visit.getInitialPayment());
				visitMap.put("paymentTerms", visit.getPaymentTerms());

			}

			visitMap.put("currentSystem", visit.getCurrentSystem());
			visitMap.put("requiredplatform", visit.getRequiredplatform());
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
			List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisited(visit);
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
			visitMap.put("designation", visit.getDesignation());
			visitMap.put("emailId", visit.getEmailId());
			visitMap.put("schoolStrenght", visit.getSchoolStrenght());
			visitMap.put("decisionMakerName", visit.getDecisionMakerName());
			visitMap.put("decisionTimeline", visit.getDecisionTimeline());
			visitMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
			visitMap.put("orderBookingDate", visit.getOrderBookingDate());
			visitMap.put("initialPayment", visit.getInitialPayment());
			visitMap.put("paymentTerms", visit.getPaymentTerms());
			visitMap.put("costPerMember", visit.getCostPerMember());
			visitMap.put("currentSystem", visit.getCurrentSystem());
			visitMap.put("requiredplatform", visit.getRequiredplatform());
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
			visitMap.put( "billingFrequency",visit.getBillingfrequency()); // "Monthly", "Quarterly",

			visitMap.put("rejectedreason", visit.getRejectionReason());
			if (visit.getStatus() == SchoolVisited.VisitStatus.REJECTED) {
				visitMap.put("rejectionReason", visit.getRejectionReason());
			}
			visitMap.put("createdAt", visit.getCreatedAt());

			// Get selected modules for this visit
			List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisited(visit);
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

	public Map<String, Object> adminUpdateSchoolVisit(Integer visitId, Map<String, Object> visitData) {
		SchoolVisited visit = schoolVisitedRepository.findById(visitId)
				.orElseThrow(() -> new IllegalArgumentException("School visit not found"));

		// Update basic/editable fields (based on getAllSchoolVisitsForAdmin)
		if (visitData.containsKey("schoolName")) {
			visit.setSchoolName((String) visitData.get("schoolName"));
		}
		if (visitData.containsKey("visitedDate") && visitData.get("visitedDate") != null) {
			visit.setVisitedDate(LocalDate.parse((String) visitData.get("visitedDate")));
		}
		if (visitData.containsKey("marketingExecutiveName")) {
			visit.setMarketingExecutiveName((String) visitData.get("marketingExecutiveName"));
		}
		if (visitData.containsKey("locationCity")) {
			visit.setLocationCity((String) visitData.get("locationCity"));
		}

		if (visitData.containsKey("contactPersonName")) {
			visit.setContactPersonName((String) visitData.get("contactPersonName"));
		}
		if (visitData.containsKey("contactNo")) {
			visit.setContactNo((String) visitData.get("contactNo"));
		}
		if (visitData.containsKey("emailId")) {
			visit.setEmailId((String) visitData.get("emailId"));
		}
		if (visitData.containsKey("designation")) {
			visit.setDesignation((String) visitData.get("designation"));
		}

		if (visitData.containsKey("schoolStrenght")) {
			visit.setSchoolStrenght((Integer) visitData.get("schoolStrenght"));
		}
		if (visitData.containsKey("expectedGoLiveDate") && visitData.get("expectedGoLiveDate") != null) {
			visit.setExpectedGoLiveDate(LocalDate.parse((String) visitData.get("expectedGoLiveDate")));
		}
		if (visitData.containsKey("orderBookingDate") && visitData.get("orderBookingDate") != null) {
			visit.setOrderBookingDate(LocalDate.parse((String) visitData.get("orderBookingDate")));
		}
		if (visitData.containsKey("initialPayment") && visitData.get("initialPayment") != null) {
			visit.setInitialPayment(new BigDecimal(visitData.get("initialPayment").toString()));
		}
		if (visitData.containsKey("paymentTerms")) {
			visit.setPaymentTerms((String) visitData.get("paymentTerms"));
			
		}
		if (visitData.containsKey("billingFrequency")) {
			visit.setBillingfrequency((String) visitData.get("billingFrequency"));
		}
		if (visitData.containsKey("costPerMember") && visitData.get("costPerMember") != null) {
			visit.setCostPerMember(new BigDecimal(visitData.get("costPerMember").toString()));
		}
		if (visitData.containsKey("currentSystem")) {
			visit.setCurrentSystem((String) visitData.get("currentSystem"));
		}
		if (visitData.containsKey("requiredplatform")) {
			visit.setRequiredplatform((String) visitData.get("requiredplatform"));
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
			visit.setCustomFeatureDescription((String) visitData.get("customFeatureDescription"));
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

		if (visitData.containsKey("decisionMakerName")) {
			visit.setDecisionMakerName((String) visitData.get("decisionMakerName"));
		}
		if (visitData.containsKey("decisionTimeline")) {
			visit.setDecisionTimeline((String) visitData.get("decisionTimeline"));
		}

		// Status handling (admin can set any status)
		if (visitData.containsKey("status") && visitData.get("status") != null) {
			String statusStr = ((String) visitData.get("status")).toUpperCase();
			SchoolVisited.VisitStatus newStatus = SchoolVisited.VisitStatus.valueOf(statusStr);

			if (newStatus == SchoolVisited.VisitStatus.REJECTED) {
				String rejectionReason = (String) visitData.get("rejectionReason");
				if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
					throw new IllegalArgumentException("Rejection reason is required when rejecting a visit");
				}
				visit.setRejectionReason(rejectionReason.trim());
			} else {
				// clear rejection reason if moving away from REJECTED
				visit.setRejectionReason(null);
			}

			visit.setStatus(newStatus);
			if (newStatus == SchoolVisited.VisitStatus.ACCEPTED) {
				// If admin provides orderBookingDate use it; otherwise set now
				if (visit.getOrderBookingDate() == null) {
					visit.setOrderBookingDate(LocalDate.now());
				}
				if (visitData.containsKey("initialPayment") && visitData.get("initialPayment") != null) {
					visit.setInitialPayment(new BigDecimal(visitData.get("initialPayment").toString()));
				}
				if (visitData.containsKey("paymentTerms")) {
					visit.setPaymentTerms((String) visitData.get("paymentTerms"));
				}
				if (visitData.containsKey("costPerMember") && visitData.get("costPerMember") != null) {
					visit.setCostPerMember(new BigDecimal(visitData.get("costPerMember").toString()));
				}
			}
		}

		// Persist visit
		schoolVisitedRepository.save(visit);
		logger.info("Admin updated school visit: {}", visitId);

		Map<String, Object> response = new HashMap<>();
		response.put("message", "School visit updated by admin successfully");
		response.put("visitId", visitId);
		response.put("status", visit.getStatus() != null ? visit.getStatus().name() : null);
		if (visitData.containsKey("selectedModules")) {
			List<Map<String, Object>> modules = (List<Map<String, Object>>) visitData.get("selectedModules");

			// delete old modules
			schoolModuleRequiredRepository.deleteBySchoolVisited(visit);

			// add new ones
			for (Map<String, Object> moduleData : modules) {
				SchoolModuleRequired schoolModule = new SchoolModuleRequired();
				schoolModule.setSchoolVisited(visit); // RELATIONSHIP
				schoolModule.setModuleId((Integer) moduleData.get("moduleId"));
				schoolModule
						.setIsSelected(SchoolModuleRequired.IsSelected.valueOf((String) moduleData.get("isSelected")));
				schoolModule.setRemarks((String) moduleData.get("remarks"));

				schoolModuleRequiredRepository.save(schoolModule);
			}
		}

		return response;
	}

	public List<Map<String, Object>> getAcceptedOrders() {
		List<SchoolVisited> acceptedVisits = schoolVisitedRepository.findByStatus(SchoolVisited.VisitStatus.ACCEPTED);
		logger.info("Retrieved {} accepted orders", acceptedVisits.size());
		List<Map<String, Object>> orderList = new ArrayList<>();

		for (SchoolVisited visit : acceptedVisits) {
			Map<String, Object> orderMap = new HashMap<>();
			orderMap.put("id", visit.getId());
			orderMap.put("schoolName", visit.getSchoolName());
			orderMap.put("visitedDate", visit.getVisitedDate());
			orderMap.put("orderBookingDate", visit.getOrderBookingDate());
			orderMap.put("expectedGoLiveDate", visit.getExpectedGoLiveDate());
			orderMap.put("schoolStrenght", visit.getSchoolStrenght());
			orderMap.put("budgetRange", visit.getBudgetRange());
			orderMap.put("initialPayment", visit.getInitialPayment());
			orderMap.put("decisionMakerName", visit.getDecisionMakerName());
			orderMap.put("decisionTimeline", visit.getDecisionTimeline());
//            orderMap.put("No Of Users", visit.getNoOfUsers());

			orderMap.put("paymentTerms", visit.getPaymentTerms());
			orderMap.put("costPerMember", visit.getCostPerMember());
			orderMap.put("currentSystem", visit.getCurrentSystem());
			orderMap.put("requiredplatform", visit.getRequiredplatform());
			orderMap.put("noOfUsers", visit.getNoOfUsers());
			orderMap.put("idCards", visit.getIdCards());
			orderMap.put("rfidIntegration", visit.getRfidIntegration());
			orderMap.put("paymentGatewayPreference", visit.getPaymentGatewayPreference());
			orderMap.put("demoRequired", visit.getDemoRequired());
			orderMap.put("demoDate", visit.getDemoDate());
			orderMap.put("proposalDate", visit.getProposalDate());
			orderMap.put("dataMigrationRequired", visit.getDataMigrationRequired());
			orderMap.put("customFeaturesRequired", visit.getCustomFeaturesRequired());
			orderMap.put("customFeatureDescription", visit.getCustomFeatureDescription());

			orderMap.put("marketingExecutiveName", visit.getMarketingExecutiveName());
			orderMap.put("locationCity", visit.getLocationCity());
			orderMap.put("contactPersonName", visit.getContactPersonName());
			orderMap.put("designation", visit.getDesignation());
			orderMap.put("contactNo", visit.getContactNo());
			orderMap.put("emailId", visit.getEmailId());
			orderMap.put("proposalSent", visit.getProposalSent());
			orderMap.put("status", visit.getStatus().name());
			orderMap.put("createdAt", visit.getCreatedAt());
			orderMap.put( "billingFrequency",visit.getBillingfrequency()); // "Monthly", "Quarterly",


			// Get selected modules
			List<SchoolModuleRequired> modules = schoolModuleRequiredRepository.findBySchoolVisited(visit);
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

	private void validateStatusTransition(SchoolVisited.VisitStatus current, SchoolVisited.VisitStatus next) {

		if (current == SchoolVisited.VisitStatus.ACCEPTED) {
			throw new IllegalStateException("Accepted visits cannot change status");
		}

		if (current == SchoolVisited.VisitStatus.REJECTED && next != SchoolVisited.VisitStatus.PENDING) {
			throw new IllegalStateException("Rejected visit can only move back to PENDING");
		}
	}

	public Map<String, Object> changeVisitStatus(Integer visitId, String newStatusStr, Map<String, Object> statusData) {
		SchoolVisited visit = schoolVisitedRepository.findById(visitId)
				.orElseThrow(() -> new IllegalArgumentException("School visit not found"));

		// Safe enum parsing
		SchoolVisited.VisitStatus newStatus = parseStatus(newStatusStr);

		validateStatusTransition(visit.getStatus(), newStatus);

		Map<String, Object> data = (statusData != null) ? statusData : Map.of();

		// Handle rejection
		if (newStatus == SchoolVisited.VisitStatus.REJECTED) {
			String reason = (String) data.get("rejectionReason");
			if (reason == null || reason.trim().isEmpty()) {
				throw new IllegalArgumentException("Rejection reason is required");
			}
			visit.setRejectionReason(reason.trim());
		} else {
			visit.setRejectionReason(null);
		}

		// Handle acceptance
		if (newStatus == SchoolVisited.VisitStatus.ACCEPTED) {
			if (visit.getOrderBookingDate() == null) {
				visit.setOrderBookingDate(LocalDate.now());
			}

			BigDecimal initialPayment = toBigDecimal(data.get("initialPayment"));
			String paymentTerms = (String) data.get("paymentTerms");
			BigDecimal costPerMember = toBigDecimal(data.get("costPerMember"));

			if (initialPayment == null || initialPayment.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Initial payment is required and must be positive");
			}
			if (paymentTerms == null || paymentTerms.trim().isEmpty()) {
				throw new IllegalArgumentException("Payment terms are required");
			}
			if (costPerMember == null || costPerMember.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Cost per member is required and must be positive");
			}

			visit.setInitialPayment(initialPayment);
			visit.setPaymentTerms(paymentTerms.trim());
			visit.setCostPerMember(costPerMember);
		}

		visit.setStatus(newStatus);
		schoolVisitedRepository.save(visit);
		Map<String, Object> response = new HashMap<>();
		response.put("message", "Status updated successfully");
		response.put("visitId", visitId);
		response.put("newStatus", newStatus.name());
		response.put("orderBookingDate", visit.getOrderBookingDate()); // can be null

		return response;
	}

	private SchoolVisited.VisitStatus parseStatus(String statusStr) {
		if (statusStr == null)
			throw new IllegalArgumentException("Status cannot be null");
		try {
			return SchoolVisited.VisitStatus.valueOf(statusStr.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid visit status: " + statusStr);
		}
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null)
			return null;
		String s = value.toString().trim();
		if (s.isEmpty())
			return null;

		try {
			s = s.replace(",", "."); // handle comma as decimal separator
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number format: " + value);
		}
	}

	public void deleteschoolVisit(Integer visitId) {
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

		// 1️⃣ Fetch user
		User user = userRepository.findByUserId(userId);
		if (user == null || user.getRole() != User.Role.MARKETING) {
			throw new IllegalArgumentException("Marketing user not found");
		}

		// 2️⃣ Fetch marketing profile via RELATIONSHIP
		MarketingTeam team = marketingTeamRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("Marketing profile not found"));

		Map<String, Object> profile = new HashMap<>();

		// Personal info
		profile.put("userId", user.getUserId());
		profile.put("profilePhotoPath", team.getProfilePhotoPath());
		profile.put("fullName", team.getFullName());
		profile.put("email", team.getEmail());
		profile.put("phoneNumber", team.getPhoneNumber());
		profile.put("age", team.getAge());
		profile.put("gender", user.getGender() != null ? user.getGender().name() : null);
		profile.put("address", team.getAddress());

		// Work info
		profile.put("designation", team.getDesignation());
		profile.put("assignedRegion", team.getAssignedRegion());
		profile.put("targetDistricts", team.getTargetDistricts());

		// Account info
		profile.put("role", user.getRole().name());
		profile.put("status", user.getStatus().name());
		profile.put("memberSince", user.getCreatedAt());

		logger.debug("Profile fetched for user: {}", userId);
		return profile;
	}

}