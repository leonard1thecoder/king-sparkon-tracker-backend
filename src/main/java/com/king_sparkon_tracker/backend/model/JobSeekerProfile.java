package com.king_sparkon_tracker.backend.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_seeker_profiles", uniqueConstraints = {
		@UniqueConstraint(name = "uk_job_seeker_profiles_user", columnNames = "user_id")
})
public class JobSeekerProfile {

	private static final int MAX_INTERESTS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private TrackerUser user;

	@Enumerated(EnumType.STRING)
	@Column(name = "highest_qualification", nullable = false, length = 40)
	private QualificationLevel highestQualification;

	@Column(name = "interested_job_one", nullable = false, length = 120)
	private String interestedJobOne;

	@Column(name = "interested_job_two", length = 120)
	private String interestedJobTwo;

	@Column(name = "interested_job_three", length = 120)
	private String interestedJobThree;

	@Column(name = "interested_job_four", length = 120)
	private String interestedJobFour;

	@Column(name = "interested_job_five", length = 120)
	private String interestedJobFive;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private JobExperienceLevel experience;

	@Column(length = 2000)
	private String about;

	@Column(name = "profile_visible_to_businesses", nullable = false)
	private boolean profileVisibleToBusinesses;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	protected JobSeekerProfile() {
	}

	public JobSeekerProfile(
			TrackerUser user,
			QualificationLevel highestQualification,
			List<String> interestedJobs,
			JobExperienceLevel experience,
			String about) {
		this(user, highestQualification, interestedJobs, experience, about, false);
	}

	public JobSeekerProfile(
			TrackerUser user,
			QualificationLevel highestQualification,
			List<String> interestedJobs,
			JobExperienceLevel experience,
			String about,
			boolean profileVisibleToBusinesses) {
		this.user = user;
		updateProfile(highestQualification, interestedJobs, experience, about, profileVisibleToBusinesses);
	}

	public void updateProfile(
			QualificationLevel highestQualification,
			List<String> interestedJobs,
			JobExperienceLevel experience,
			String about) {
		updateProfile(highestQualification, interestedJobs, experience, about, profileVisibleToBusinesses);
	}

	public void updateProfile(
			QualificationLevel highestQualification,
			List<String> interestedJobs,
			JobExperienceLevel experience,
			String about,
			boolean profileVisibleToBusinesses) {
		this.highestQualification = highestQualification;
		this.experience = experience;
		this.about = about;
		this.profileVisibleToBusinesses = profileVisibleToBusinesses;
		this.interestedJobOne = interestedJobs.get(0);
		this.interestedJobTwo = interestedJobs.size() > 1 ? interestedJobs.get(1) : null;
		this.interestedJobThree = interestedJobs.size() > 2 ? interestedJobs.get(2) : null;
		this.interestedJobFour = interestedJobs.size() > 3 ? interestedJobs.get(3) : null;
		this.interestedJobFive = interestedJobs.size() > 4 ? interestedJobs.get(4) : null;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public TrackerUser getUser() {
		return user;
	}

	public QualificationLevel getHighestQualification() {
		return highestQualification;
	}

	public List<String> getInterestedJobs() {
		List<String> interests = new ArrayList<>(MAX_INTERESTS);
		addInterest(interests, interestedJobOne);
		addInterest(interests, interestedJobTwo);
		addInterest(interests, interestedJobThree);
		addInterest(interests, interestedJobFour);
		addInterest(interests, interestedJobFive);
		return List.copyOf(interests);
	}

	private void addInterest(List<String> interests, String interest) {
		if (interest != null && !interest.isBlank()) {
			interests.add(interest);
		}
	}

	public JobExperienceLevel getExperience() {
		return experience;
	}

	public String getAbout() {
		return about;
	}

	public boolean isProfileVisibleToBusinesses() {
		return profileVisibleToBusinesses;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}
}
