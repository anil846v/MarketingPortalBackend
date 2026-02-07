package com.example.visited.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// AdmiService imports:
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile; // For @RequestPart

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

@Service
@Transactional // ✅ Class-level for all CRUD
public class AdmiService {

	@Value("${app.upload.dir:./uploads/marketing}") // ← ADD THIS
	private String uploadDir; // ← Make it instance variable

	private static final Logger logger = LoggerFactory.getLogger(AdmiService.class);

	private final UserRepository userRepository;
	private final MarketingTeamRepository marketingTeamRepository;
	private final ModulesRepository modulesRepository;
	private final BCryptPasswordEncoder passwordEncoder;
	private final SchoolsVisitedRepository schoolVisitedRepository;
	private final SchoolModuleRequiredRepository schoolModuleRequiredRepository;



	public AdmiService(UserRepository userRepository, MarketingTeamRepository marketingTeamRepository,
			ModulesRepository modulesRepository, BCryptPasswordEncoder passwordEncoder,SchoolsVisitedRepository schoolVisitedRepository,SchoolModuleRequiredRepository schoolModuleRequiredRepository) {
		this.userRepository = userRepository;
		this.marketingTeamRepository = marketingTeamRepository;
		this.modulesRepository = modulesRepository;
		this.passwordEncoder = passwordEncoder;
		this.schoolVisitedRepository  =schoolVisitedRepository;
		this.schoolModuleRequiredRepository = schoolModuleRequiredRepository;
	}

	// ── Marketing Users ─────────────────────────────────────────────
	@Transactional
	public List<Map<String, Object>> getAllMarketingUsers() {
		List<User> marketingUsers = userRepository.findByRole(User.Role.MARKETING);

		List<Map<String, Object>> result = new ArrayList<>();
		for (User user : marketingUsers) {
			Map<String, Object> map = new HashMap<>();
			map.put("userId", user.getUserId());
			map.put("username", user.getUsername());
			map.put("role", user.getRole().name());
			map.put("gender", user.getGender() != null ? user.getGender().name() : null);
			map.put("status", user.getStatus() != null ? user.getStatus().name() : null);

			marketingTeamRepository.findByUser(user).ifPresentOrElse(team -> {
				map.put("fullName", team.getFullName());
				map.put("phoneNumber", team.getPhoneNumber());
				map.put("email", team.getEmail());
				map.put("age", team.getAge());
				map.put("address", team.getAddress());
				map.put("assignedRegion", team.getAssignedRegion());
				map.put("targetDistricts", team.getTargetDistricts());
				map.put("profilePhotoPath", team.getProfilePhotoPath());
			}, () -> map.put("profile", "No profile found"));

			result.add(map);
		}

		logger.info("Fetched {} marketing users", result.size());
		return result;
	}

	@Transactional
	public User registerMarketingUser(Map<String, Object> userData) {
		String email = (String) userData.get("email");

		if (userRepository.findByUsername(email) != null) {
			throw new IllegalArgumentException("Email already registered");
		}
		if (marketingTeamRepository.findByPhoneNumber((String) userData.get("phoneNumber")) != null) {
			throw new IllegalArgumentException("Phone number already exists");
		}

		User user = new User();
		user.setUsername(email);
		user.setPasswordHash(passwordEncoder.encode((String) userData.get("password")));
		user.setGender(User.Gender.valueOf((String) userData.get("gender")));
		user.setRole(User.Role.MARKETING);
		user.setStatus(User.Status.Approved);

		User saved = userRepository.save(user);

		MarketingTeam team = new MarketingTeam();
		team.setUser(saved);   // NOT setUserId()
		logger.info("user" + user);
		team.setFullName((String) userData.get("fullName"));
		team.setPhoneNumber((String) userData.get("phoneNumber"));
		team.setEmail(email);

		// Handle age conversion
		Object ageObj = userData.get("age");
		if (ageObj instanceof Integer) {
			team.setAge((Integer) ageObj);
		} else if (ageObj instanceof String) {
			team.setAge(Integer.parseInt((String) ageObj));
		}

		team.setAddress((String) userData.get("address"));
		team.setAssignedRegion((String) userData.get("assignedRegion"));
		team.setTargetDistricts((String) userData.get("targetDistricts"));
		//  NEW: PHOTO UPLOAD (20 lines max)
		//  FIXED PHOTO UPLOAD
		MultipartFile photo = (MultipartFile) userData.get("photo");
		if (photo != null && !photo.isEmpty()) {
			// Security + Validation
			if (photo.getSize() > 5 * 1024 * 1024) { // 5MB
				throw new IllegalArgumentException("Photo too large (max 5MB)");
			}
			if (!isImageFile(photo)) {
				throw new IllegalArgumentException("Only JPG/PNG allowed");
			}

			try {
				//  FIXED: Create directory + filename + save
				Files.createDirectories(Paths.get(uploadDir));
				String filename = saved.getUserId() + "_" + System.currentTimeMillis()
						+ getFileExtension(photo.getOriginalFilename());
				Path filePath = Paths.get(uploadDir, filename); // ✅ Now filename exists
				photo.transferTo(filePath);
				team.setProfilePhotoPath("/uploads/marketing/" + filename);

			} catch (Exception e) {
				logger.error("Photo upload failed for user: {}", email, e);
				throw new RuntimeException("Photo upload failed", e);
			}
		}

		marketingTeamRepository.save(team);

		return saved;
	}

	// Add these helper methods to AdmiService
	private boolean isImageFile(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png")
				|| contentType.equals("image/jpg") || contentType.equals("image/webp"));
	}

	private String getFileExtension(String filename) {
		return filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : ".jpg";
	}

	@Transactional
	public User updateMarketingUser(Integer userId, Map<String, Object> userData) {

	    User user = userRepository.findByUserId(userId);
	    if (user == null || user.getRole() != User.Role.MARKETING) {
	        throw new IllegalArgumentException("Marketing user not found");
	    }

	    // ---------- USER TABLE ----------
	    String newEmail = (String) userData.get("email");
	    if (newEmail != null && !newEmail.equals(user.getUsername())) {
	        if (userRepository.findByUsername(newEmail) != null) {
	            throw new IllegalArgumentException("Email already in use");
	        }
	        user.setUsername(newEmail);
	    }

	    String newPhone = (String) userData.get("phoneNumber");
	    if (newPhone != null) {
	        MarketingTeam t = marketingTeamRepository.findByPhoneNumber(newPhone);
	        if (t != null && !t.getUser().getUserId().equals(userId)) {
	            throw new IllegalArgumentException("Phone already in use");
	        }
	    }

	    if (userData.containsKey("password")) {
	        user.setPasswordHash(passwordEncoder.encode((String) userData.get("password")));
	    }

	    if (userData.containsKey("gender")) {
	        user.setGender(User.Gender.valueOf((String) userData.get("gender")));
	    }

	    if (userData.containsKey("status")) {
	        user.setStatus(User.Status.valueOf((String) userData.get("status")));
	    }

	    User savedUser = userRepository.save(user);

	    // ---------- MARKETING TEAM TABLE ----------
	    marketingTeamRepository.findByUser(user).ifPresent(team -> {

	        if (userData.containsKey("fullName"))
	            team.setFullName((String) userData.get("fullName"));

	        if (newPhone != null)
	            team.setPhoneNumber(newPhone);

	        if (userData.containsKey("email"))
	            team.setEmail((String) userData.get("email"));

	        if (userData.containsKey("age")) {
	            Object ageObj = userData.get("age");
	            if (ageObj instanceof Integer) {
	                team.setAge((Integer) ageObj);
	            } else if (ageObj instanceof String) {
	                team.setAge(Integer.parseInt((String) ageObj));
	            }
	        }

	        if (userData.containsKey("address"))
	            team.setAddress((String) userData.get("address"));

	        if (userData.containsKey("assignedRegion"))
	            team.setAssignedRegion((String) userData.get("assignedRegion"));

	        if (userData.containsKey("targetDistricts"))
	            team.setTargetDistricts((String) userData.get("targetDistricts"));

	        // ---------- PHOTO UPLOAD (FIXED) ----------
	        MultipartFile photo = (MultipartFile) userData.get("photo");

	        // ONLY if new photo is provided
	        if (photo != null && !photo.isEmpty()) {

	            if (photo.getSize() > 5 * 1024 * 1024)
	                throw new IllegalArgumentException("Photo too large (max 5MB)");

	            if (!isImageFile(photo))
	                throw new IllegalArgumentException("Only JPG/PNG allowed");

	            // Delete old photo ONLY now
	            String oldPhotoPath = team.getProfilePhotoPath();
	            if (oldPhotoPath != null) {
	                try {
	                    Path oldFile = Paths.get(
	                            uploadDir,
	                            oldPhotoPath.replace("/uploads/marketing/", "")
	                    );
	                    Files.deleteIfExists(oldFile);
	                } catch (Exception e) {
	                    logger.warn("Could not delete old photo for user {}", team.getEmail(), e);
	                }
	            }

	            try {
	                Files.createDirectories(Paths.get(uploadDir));
	                String filename = userId + "_" + System.currentTimeMillis()
	                        + getFileExtension(photo.getOriginalFilename());
	                Path filePath = Paths.get(uploadDir, filename);

	                photo.transferTo(filePath);
	                team.setProfilePhotoPath("/uploads/marketing/" + filename);

	            } catch (Exception e) {
	                logger.error("Photo upload failed for user: {}", team.getEmail(), e);
	                throw new RuntimeException("Photo upload failed", e);
	            }
	        }

	        marketingTeamRepository.save(team);
	    });

	    return savedUser;
	}

	@Transactional
	public void deleteMarketingUser(Integer userId) {

	    User user = userRepository.findByUserId(userId);
	    if (user == null || user.getRole() != User.Role.MARKETING) {
	        throw new IllegalArgumentException("Marketing user not found");
	    }

	    marketingTeamRepository.findByUser(user).ifPresent(team -> {

	        // 1️⃣ Delete photo from disk if exists
	        String photoPath = team.getProfilePhotoPath();

	        if (photoPath != null) {
	            try {
	                Path filePath = Paths.get(uploadDir, photoPath.replace("/uploads/marketing/", ""));
	                Files.deleteIfExists(filePath);
	                logger.info("Deleted profile photo for user {}", team.getEmail());
	            } catch (Exception e) {
	                logger.warn("Could not delete photo for user {}", team.getEmail(), e);
	            }
	        }

	        // 2️⃣ Delete marketing team record
	        marketingTeamRepository.delete(team);
	    });

	    // 3️⃣ Delete user (JWT, visits, etc cascade if FK configured)
	    userRepository.delete(user);
	}

	
	public List<Map<String, Object>> getschoolsbyUser(Integer userId) {
		User user = userRepository.findByUserId(userId);
	    if (user == null || user.getRole() != User.Role.MARKETING) {
	        throw new IllegalArgumentException("Marketing user not found");
	    }
			List<SchoolVisited> visits = schoolVisitedRepository.findByUser(user);
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


	    
		
		
	
	// ── Modules ─────────────────────────────────────────────────────
	@Transactional
	public List<Modules> getAllModules() {
		return modulesRepository.findAll();
	}

	@Transactional
	public Modules createModule(Map<String, Object> moduleData) {
		String moduleName = (String) moduleData.get("moduleName");
		if (moduleName == null || moduleName.trim().isEmpty()) {
			throw new IllegalArgumentException("Module name is required");
		}

		Modules module = new Modules();
		module.setModuleName(moduleName.trim());
		module.setDescription((String) moduleData.get("description"));

		Object isActiveObj = moduleData.get("isActive");
		if (isActiveObj instanceof Boolean) {
			module.setIsActive((Boolean) isActiveObj);
		} else {
			module.setIsActive(true); // Default to active
		}

		return modulesRepository.save(module);
	}

	@Transactional
	public Modules updateModule(Integer moduleId, Map<String, Object> moduleData) {
	    try {
	        Modules module = modulesRepository.findById(moduleId)
	                .orElseThrow(() -> new IllegalArgumentException("Module not found with ID: " + moduleId));

	        if (moduleData.containsKey("moduleName")) {
	            String name = ((String) moduleData.get("moduleName")).trim();
	            if (name.isEmpty()) {
	                throw new IllegalArgumentException("Module name cannot be empty");
	            }
	            module.setModuleName(name);
	        }

	        if (moduleData.containsKey("description")) {
	            module.setDescription((String) moduleData.get("description"));
	        }

	        if (moduleData.containsKey("isActive")) {
	            Object val = moduleData.get("isActive");
	            if (val instanceof Boolean) {
	                module.setIsActive((Boolean) val);
	            } else {
	                throw new IllegalArgumentException("isActive must be a boolean value");
	            }
	        }

	        return modulesRepository.save(module);

	    } catch (IllegalArgumentException e) {
	        // Let this bubble up to controller → becomes 400 Bad Request
	        throw e;

	    } catch (Exception e) {
	        // Any unexpected issue during save (constraint violation, DB down, etc.)
	        logger.error("Unexpected error updating module id: {}", moduleId, e);
	        throw new RuntimeException("Failed to update module", e);
	    }
	}

	

	// ── Admin Profile ──────────────────────────────────────────────
	@Transactional(readOnly = true)
	public Map<String, Object> getAdminProfile(Integer userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		User admin = userRepository.findByUserId(userId);
		if (admin == null || admin.getRole() != User.Role.ADMIN) {
			throw new IllegalArgumentException("Admin user not found");
		}

		Map<String, Object> profile = new HashMap<>();
		profile.put("userId", admin.getUserId());
		profile.put("username", admin.getUsername());
		profile.put("role", admin.getRole().name());
		profile.put("status", admin.getStatus().name());
		profile.put("gender", admin.getGender() != null ? admin.getGender().name() : null);
		profile.put("createdAt", admin.getCreatedAt());
		profile.put("updatedAt", admin.getUpdatedAt());
		marketingTeamRepository.findByUser(admin)
        .ifPresent(team -> profile.put("profilePhotoPath", team.getProfilePhotoPath()));


		logger.debug("Admin profile fetched for user: {}", userId);
		return profile;
	}

	@Transactional
	public Map<String, Object> updateAdminProfile(Integer userId, Map<String, Object> profileData) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		User admin = userRepository.findByUserId(userId);
		if (admin == null || admin.getRole() != User.Role.ADMIN) {
			throw new IllegalArgumentException("Admin user not found");
		}

		// Validate and update username/email
		if (profileData.containsKey("username")) {
			String newUsername = ((String) profileData.get("username")).trim();
			if (newUsername.isEmpty() || newUsername.length() < 3) {
				throw new IllegalArgumentException("Username must be at least 3 characters");
			}
			if (!newUsername.equals(admin.getUsername())) {
				User existing = userRepository.findByUsername(newUsername);
				if (existing != null) {
					throw new IllegalArgumentException("Username already exists");
				}
				admin.setUsername(newUsername);
			}
		}

		// Update password if provided
		if (profileData.containsKey("password")) {
			String newPassword = (String) profileData.get("password");
			if (newPassword == null || newPassword.length() < 6) {
				throw new IllegalArgumentException("Password must be at least 6 characters");
			}
			admin.setPasswordHash(passwordEncoder.encode(newPassword));
		}

		// Update gender if provided
		if (profileData.containsKey("gender")) {
			String genderStr = (String) profileData.get("gender");
			if (genderStr != null && !genderStr.trim().isEmpty()) {
				try {
					admin.setGender(User.Gender.valueOf(genderStr.trim()));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid gender value");
				}
			}
		}

		// --- PHOTO UPLOAD ---
		MultipartFile photo = (MultipartFile) profileData.get("photo");
		if (photo != null && !photo.isEmpty()) {
			if (photo.getSize() > 5 * 1024 * 1024)
				throw new IllegalArgumentException("Photo too large (max 5MB)");
			if (!isImageFile(photo))
				throw new IllegalArgumentException("Only JPG/PNG allowed");

			try {
				Files.createDirectories(Paths.get(uploadDir));
				String filename = userId + "_" + System.currentTimeMillis()
						+ getFileExtension(photo.getOriginalFilename());
				Path filePath = Paths.get(uploadDir, filename);
				photo.transferTo(filePath);

				// Save profile photo path
				marketingTeamRepository.findByUser(admin).ifPresent(team -> {
					team.setProfilePhotoPath("/uploads/marketing/" + filename);
					marketingTeamRepository.save(team);
				});

			} catch (Exception e) {
				logger.error("Admin photo upload failed for user: {}", admin.getUsername(), e);
				throw new RuntimeException("Photo upload failed", e);
			}
		}

		User updatedAdmin = userRepository.save(admin);
		logger.info("Admin profile updated for user: {}", userId);

		return Map.of("message", "Profile updated successfully", "userId", updatedAdmin.getUserId(), "username",
				updatedAdmin.getUsername());
	}

	
}