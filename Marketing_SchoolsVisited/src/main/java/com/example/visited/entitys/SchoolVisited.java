package com.example.visited.entitys;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_visited")
public class SchoolVisited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "school_name", length = 70)
    private String schoolName;

    @Column(name = "visited_date")
    private LocalDate visitedDate;

    // ── NEW: JPA relationship to User ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK to users.user_id
    private User user;

    @Column(name = "marketing_executive_name", length = 50)
    private String marketingExecutiveName;

    @Column(name = "location_city", columnDefinition = "TEXT")
    private String locationCity;

    @Column(name = "contact_person_name", columnDefinition = "TEXT")
    private String contactPersonName;

    @Column(columnDefinition = "TEXT")
    private String designation;

    @Column(name = "contact_no", columnDefinition = "TEXT")
    private String contactNo;

    @Column(name = "email_id", columnDefinition = "TEXT")
    private String emailId;

    @Column(name = "school_strenght")
    private Integer schoolStrenght;

    @Column(columnDefinition = "TEXT")
    private String boards;

    @Column(name = "current_system", length = 100)
    private String currentSystem;

    @Column(name = "no_of_users")
    private Integer noOfUsers;

    @Column(name = "data_migration_required", length = 30)
    private String dataMigrationRequired;

    @Column(name = "custom_features_required", columnDefinition = "TEXT")
    private String customFeaturesRequired;

    @Column(name = "custom_feature_description")
    private String customFeatureDescription;

    @Column(name = "requiredplatform")
    private String requiredplatform;

    @Column(name = "billingfrequency")
    private String billingfrequency;
   
   

	@Column(name = "rfid_integration", length = 40)
    private String rfidIntegration;

    @Column(name = "id_cards", length = 45)
    private String idCards;

    @Column(name = "payment_gateway_preference", columnDefinition = "TEXT")
    private String paymentGatewayPreference;

    @Column(name = "budget_range", precision = 12, scale = 2)
    private BigDecimal budgetRange = BigDecimal.ZERO;

    @Column(name = "expected_go_live_date")
    private LocalDate expectedGoLiveDate;

    @Column(name = "order_booking_date")
    private LocalDate orderBookingDate;

    @Column(name = "initial_payment", precision = 12, scale = 2)
    private BigDecimal initialPayment;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "cost_per_member", precision = 10, scale = 2)
    private BigDecimal costPerMember;

    @Column(name = "decision_maker_name", length = 50)
    private String decisionMakerName;

    @Column(name = "decision_timeline", columnDefinition = "TEXT")
    private String decisionTimeline;

    @Column(name = "demo_required", length = 50)
    private String demoRequired;

    @Column(name = "demo_date")
    private LocalDate demoDate;

    @Column(name = "proposal_sent", length = 50)
    private String proposalSent;

    @Column(name = "proposal_date")
    private LocalDate proposalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status = VisitStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── JPA Callbacks for timestamps ──
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enum ──
    public enum VisitStatus {
        PENDING, ACCEPTED, REJECTED
    }
    public String getRequiredplatform() {
		return requiredplatform;
	}

	public void setRequiredplatform(String requiredplatform) {
		this.requiredplatform = requiredplatform;
	}

    // ── Getters and Setters ──
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public LocalDate getVisitedDate() { return visitedDate; }
    public void setVisitedDate(LocalDate visitedDate) { this.visitedDate = visitedDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMarketingExecutiveName() { return marketingExecutiveName; }
    public void setMarketingExecutiveName(String marketingExecutiveName) { this.marketingExecutiveName = marketingExecutiveName; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }

    public Integer getSchoolStrenght() { return schoolStrenght; }
    public void setSchoolStrenght(Integer schoolStrenght) { this.schoolStrenght = schoolStrenght; }

    public String getBoards() { return boards; }
    public void setBoards(String boards) { this.boards = boards; }

    public String getCurrentSystem() { return currentSystem; }
    public void setCurrentSystem(String currentSystem) { this.currentSystem = currentSystem; }

    public Integer getNoOfUsers() { return noOfUsers; }
    public void setNoOfUsers(Integer noOfUsers) { this.noOfUsers = noOfUsers; }

    public String getDataMigrationRequired() { return dataMigrationRequired; }
    public void setDataMigrationRequired(String dataMigrationRequired) { this.dataMigrationRequired = dataMigrationRequired; }

    public String getCustomFeaturesRequired() { return customFeaturesRequired; }
    public void setCustomFeaturesRequired(String customFeaturesRequired) { this.customFeaturesRequired = customFeaturesRequired; }

    public String getCustomFeatureDescription() { return customFeatureDescription; }
    public void setCustomFeatureDescription(String customFeatureDescription) { this.customFeatureDescription = customFeatureDescription; }

    public String getRfidIntegration() { return rfidIntegration; }
    public void setRfidIntegration(String rfidIntegration) { this.rfidIntegration = rfidIntegration; }

    public String getIdCards() { return idCards; }
    public void setIdCards(String idCards) { this.idCards = idCards; }

    public String getPaymentGatewayPreference() { return paymentGatewayPreference; }
    public void setPaymentGatewayPreference(String paymentGatewayPreference) { this.paymentGatewayPreference = paymentGatewayPreference; }

    public BigDecimal getBudgetRange() { return budgetRange; }
    public void setBudgetRange(BigDecimal budgetRange) { this.budgetRange = budgetRange; }

    public LocalDate getExpectedGoLiveDate() { return expectedGoLiveDate; }
    public void setExpectedGoLiveDate(LocalDate expectedGoLiveDate) { this.expectedGoLiveDate = expectedGoLiveDate; }

    public LocalDate getOrderBookingDate() { return orderBookingDate; }
    public void setOrderBookingDate(LocalDate orderBookingDate) { this.orderBookingDate = orderBookingDate; }

    public BigDecimal getInitialPayment() { return initialPayment; }
    public void setInitialPayment(BigDecimal initialPayment) { this.initialPayment = initialPayment; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public BigDecimal getCostPerMember() { return costPerMember; }
    public void setCostPerMember(BigDecimal costPerMember) { this.costPerMember = costPerMember; }

    public String getDecisionMakerName() { return decisionMakerName; }
    public void setDecisionMakerName(String decisionMakerName) { this.decisionMakerName = decisionMakerName; }

    public String getDecisionTimeline() { return decisionTimeline; }
    public void setDecisionTimeline(String decisionTimeline) { this.decisionTimeline = decisionTimeline; }

    public String getDemoRequired() { return demoRequired; }
    public void setDemoRequired(String demoRequired) { this.demoRequired = demoRequired; }

    public LocalDate getDemoDate() { return demoDate; }
    public void setDemoDate(LocalDate demoDate) { this.demoDate = demoDate; }

    public String getProposalSent() { return proposalSent; }
    public void setProposalSent(String proposalSent) { this.proposalSent = proposalSent; }

    public LocalDate getProposalDate() { return proposalDate; }
    public void setProposalDate(LocalDate proposalDate) { this.proposalDate = proposalDate; }

    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
	public String getBillingfrequency() {
		return billingfrequency;
	}

	public void setBillingfrequency(String billingfrequency) {
		this.billingfrequency = billingfrequency;
	}
}
