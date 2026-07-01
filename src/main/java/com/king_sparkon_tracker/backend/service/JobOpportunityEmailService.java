package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.JobApplication;
import com.king_sparkon_tracker.backend.model.JobInterview;
import com.king_sparkon_tracker.backend.model.JobPost;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequest;
import com.king_sparkon_tracker.backend.model.TrackerUser;

import jakarta.mail.internet.MimeMessage;

@Service
public class JobOpportunityEmailService {

	private static final Logger log = LoggerFactory.getLogger(JobOpportunityEmailService.class);
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender mailSender;
	private final boolean mailEnabled;
	private final String mailFrom;

	public JobOpportunityEmailService(
			JavaMailSender mailSender,
			@Value("${app.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.mail.from:no-reply@kingsparkon-tracker.com}") String mailFrom) {
		this.mailSender = mailSender;
		this.mailEnabled = mailEnabled;
		this.mailFrom = mailFrom;
	}

	public boolean sendApplicationSubmittedOwnerEmail(JobApplication application) {
		JobPost post = application.getJobPost();
		Business business = post.getBusiness();
		TrackerUser applicant = application.getApplicant();
		String html = page(
				"New job application",
				"<p><strong>" + esc(applicant.getUsername()) + "</strong> applied for <strong>" + esc(post.getTitle()) + "</strong>.</p>"
						+ "<p>Business: " + esc(business.getName()) + "</p>"
						+ "<p>Resume: " + link(application.getResumeUrl()) + "</p>"
						+ "<p>Private qualifications and certificates stay locked until the applicant approves your access request.</p>");
		return sendHtmlEmail(ownerEmail(business), "New application for " + post.getTitle(), html, "job_application_owner_email");
	}

	public boolean sendApplicationSubmittedApplicantEmail(JobApplication application) {
		JobPost post = application.getJobPost();
		String html = page(
				"Application received",
				"<p>Your application for <strong>" + esc(post.getTitle()) + "</strong> at "
						+ esc(post.getBusiness().getName()) + " was received.</p>"
						+ "<p>Status: " + application.getStatus() + "</p>");
		return sendHtmlEmail(application.getApplicant().getEmailAddress(), "We received your King Sparkon Tracker application", html, "job_application_applicant_email");
	}

	public boolean sendApplicationDeclinedEmail(JobApplication application) {
		JobPost post = application.getJobPost();
		String html = page(
				"Application update",
				"<p>Your application for <strong>" + esc(post.getTitle()) + "</strong> at "
						+ esc(post.getBusiness().getName()) + " was not successful.</p>");
		return sendHtmlEmail(application.getApplicant().getEmailAddress(), "Update on your King Sparkon Tracker job application", html, "job_application_declined_email");
	}

	public boolean sendProfileAccessRequestedEmail(JobProfileAccessRequest request) {
		JobApplication application = request.getApplication();
		String message = request.getRequestMessage() == null || request.getRequestMessage().isBlank()
				? "No extra message was provided."
				: request.getRequestMessage();
		String html = page(
				"Profile access requested",
				"<p>" + esc(request.getBusiness().getName()) + " requested access to your qualification and certificate documents for <strong>"
						+ esc(application.getJobPost().getTitle()) + "</strong>.</p>"
						+ "<p>Message: " + esc(message) + "</p>"
						+ "<p>You can approve or decline this request from Opportunities.</p>");
		return sendHtmlEmail(request.getApplicant().getEmailAddress(), "Approve profile access for your job application", html, "job_profile_access_requested_email");
	}

	public boolean sendProfileAccessApprovedEmail(JobProfileAccessRequest request) {
		String html = page(
				"Profile access approved",
				"<p>" + esc(request.getApplicant().getUsername()) + " approved access to qualification and certificate documents for <strong>"
						+ esc(request.getApplication().getJobPost().getTitle()) + "</strong>.</p>");
		return sendHtmlEmail(ownerEmail(request.getBusiness()), "Applicant approved profile access", html, "job_profile_access_approved_email");
	}

	public boolean sendProfileAccessDeclinedEmail(JobProfileAccessRequest request) {
		String html = page(
				"Profile access declined",
				"<p>" + esc(request.getApplicant().getUsername()) + " declined access to qualification and certificate documents for <strong>"
						+ esc(request.getApplication().getJobPost().getTitle()) + "</strong>.</p>");
		return sendHtmlEmail(ownerEmail(request.getBusiness()), "Applicant declined profile access", html, "job_profile_access_declined_email");
	}

	public boolean sendInterviewBookedEmail(JobInterview interview) {
		JobPost post = interview.getJobPost();
		String html = page(
				"Interview booked",
				"<p>You have an interview for <strong>" + esc(post.getTitle()) + "</strong>.</p>"
						+ "<p>Date: " + formatDate(interview.getInterviewDate()) + "</p>"
						+ "<p>Address: " + esc(interview.getBusinessAddress()) + "</p>"
						+ "<p>Expires: " + formatDate(interview.getInterviewExpiresAt()) + "</p>"
						+ "<p>Details: " + esc(interview.getInterviewDescription()) + "</p>");
		return sendHtmlEmail(interview.getApplicant().getEmailAddress(), "Interview booked for " + post.getTitle(), html, "job_interview_booked_email");
	}

	public boolean sendInterviewAcceptedOwnerEmail(JobInterview interview) {
		String html = page(
				"Interview accepted",
				"<p>" + esc(interview.getApplicant().getUsername()) + " accepted the interview for <strong>"
						+ esc(interview.getJobPost().getTitle()) + "</strong>.</p>");
		return sendHtmlEmail(ownerEmail(interview.getBusiness()), "Interview accepted", html, "job_interview_accepted_owner_email");
	}

	public boolean sendInterviewDeclinedOwnerEmail(JobInterview interview) {
		String html = page(
				"Interview declined",
				"<p>" + esc(interview.getApplicant().getUsername()) + " declined the interview for <strong>"
						+ esc(interview.getJobPost().getTitle()) + "</strong>.</p>");
		return sendHtmlEmail(ownerEmail(interview.getBusiness()), "Interview declined", html, "job_interview_declined_owner_email");
	}

	private String page(String title, String body) {
		return "<html><body style=\"font-family:Arial,sans-serif;color:#101827;line-height:1.5\">"
				+ "<h2>" + esc(title) + "</h2>"
				+ body
				+ "<p style=\"margin-top:24px;color:#64748b\">King Sparkon Tracker</p>"
				+ "</body></html>";
	}

	private String link(String url) {
		if (url == null || url.isBlank()) {
			return "Not provided";
		}
		String safeUrl = esc(url);
		return "<a href=\"" + safeUrl + "\">" + safeUrl + "</a>";
	}

	private String formatDate(OffsetDateTime dateTime) {
		return dateTime == null ? "Not set" : DATE_TIME_FORMAT.format(dateTime);
	}

	private String ownerEmail(Business business) {
		return business.getOwner().getEmailAddress();
	}

	private boolean sendHtmlEmail(String to, String subject, String html, String eventName) {
		String normalizedTo = normalizeEmail(to);
		if (!mailEnabled) {
			log.warn("{}_preview mailEnabled=false recipient={} subject={}", eventName, AppEmailService.maskEmail(normalizedTo), subject);
			return false;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			helper.setFrom(mailFrom);
			helper.setTo(normalizedTo);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
			log.info("{}_sent recipient={}", eventName, AppEmailService.maskEmail(normalizedTo));
			return true;
		} catch (Exception ex) {
			log.error("{}_failed recipient={}", eventName, AppEmailService.maskEmail(normalizedTo), ex);
			throw new IllegalStateException("Could not send job opportunity email. Please try again later.");
		}
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private String esc(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
