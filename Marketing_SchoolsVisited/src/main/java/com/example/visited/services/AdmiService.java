package com.example.visited.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.visited.entitys.MarketingTeam;
import com.example.visited.entitys.Modules;
import com.example.visited.entitys.User;
import com.example.visited.repositories.MarketingTeamRepository;
import com.example.visited.repositories.ModulesRepository;
import com.example.visited.repositories.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // ✅ Class-level for all CRUD
public class AdmiService {

	private static final Logger logger = LoggerFactory.getLogger(AdmiService.class);

	private final UserRepository userRepository;
	private final MarketingTeamRepository marketingTeamRepository;
	private final ModulesRepository modulesRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public AdmiService(UserRepository userRepository, MarketingTeamRepository marketingTeamRepository,
			ModulesRepository modulesRepository, BCryptPasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.marketingTeamRepository = marketingTeamRepository;
		this.modulesRepository = modulesRepository;
		this.passwordEncoder = passwordEncoder;
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

			marketingTeamRepository.findByUserId(user.getUserId()).ifPresentOrElse(team -> {
				map.put("fullName", team.getFullName());
				map.put("phoneNumber", team.getPhoneNumber());
				map.put("email", team.getEmail());
				map.put("age", team.getAge());
				map.put("address", team.getAddress());
				map.put("assignedRegion", team.getAssignedRegion());
				map.put("targetDistricts", team.getTargetDistricts());
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
		team.setUserId(saved.getUserId());
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

		marketingTeamRepository.save(team);

		return saved;
	}

	@Transactional
	public User updateMarketingUser(Integer userId, Map<String, Object> userData) {
		User user = userRepository.findByUserId(userId);
		if (user == null || user.getRole() != User.Role.MARKETING) {
			throw new IllegalArgumentException("Marketing user not found");
		}

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
			if (t != null && !t.getUserId().equals(userId)) {
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

		marketingTeamRepository.findByUserId(userId).ifPresent(team -> {
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

		marketingTeamRepository.findByUserId(userId).ifPresent(team -> marketingTeamRepository.delete(team));

		userRepository.delete(user);
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
		Modules module = modulesRepository.findById(moduleId)
				.orElseThrow(() -> new IllegalArgumentException("Module not found"));

		if (moduleData.containsKey("moduleName")) {
			String name = ((String) moduleData.get("moduleName")).trim();
			if (name.isEmpty())
				throw new IllegalArgumentException("Module name cannot be empty");
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
				throw new IllegalArgumentException("isActive must be boolean");
			}
		}

		return modulesRepository.save(module);
	}

	@Transactional
	public void deleteModule(Integer moduleId) {
		Modules module = modulesRepository.findById(moduleId)
				.orElseThrow(() -> new IllegalArgumentException("Module not found"));

		modulesRepository.delete(module);
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
		
		User updatedAdmin = userRepository.save(admin);
		logger.info("Admin profile updated for user: {}", userId);
		
		return Map.of(
			"message", "Profile updated successfully",
			"userId", updatedAdmin.getUserId(),
			"username", updatedAdmin.getUsername()
		);
	}
}