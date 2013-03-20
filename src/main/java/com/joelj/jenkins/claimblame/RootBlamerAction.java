package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.User;
import hudson.tasks.Mailer;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 2/7/13
 * Time: 12:37 PM
 */
@Extension
public class RootBlamerAction implements RootAction {
	private static final Logger LOGGER = Logger.getLogger("ClaimBlame");

	public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
		String specificJob = request.getParameter("job");

		Set<String> trackedJobs;
		if(specificJob == null || specificJob.isEmpty()) {
			trackedJobs = BlamerFactory.getTrackedJobs();
		} else {
			trackedJobs = new HashSet<String>(1);
			trackedJobs.add(specificJob);
		}

		Map<String, Map<String, Map<String, String>>> jobAssignments = new HashMap<String, Map<String, Map<String, String>>>();

		for (String trackedJob : trackedJobs) {
			Blamer blamer = BlamerFactory.getBlamerForJob(trackedJob);
			blamer.load();
			Set<String> tests = blamer.getTests();
			Map<String, Map<String, String>> testAssignments = new HashMap<String, Map<String, String>>();
			for (String test : tests) {
				Map<String, String> assignment = new HashMap<String, String>();
				User culprit = blamer.getCulprit(test);
				if(culprit != null) {
					assignment.put("culprit", culprit.getId());
					Status status = blamer.getStatus(test);
					if(status != null) {
						assignment.put("status", status.toString());
					}
				}
				testAssignments.put(test, assignment);
			}
			jobAssignments.put(trackedJob, testAssignments);
		}

		JSONObject json = JSONObject.fromObject(jobAssignments);
		json.write(response.getWriter());
	}

	public void doBlame(StaplerRequest request, StaplerResponse response,
						@QueryParameter(value = "testNames", required = true) String[] testNames,
						@QueryParameter(value = "userId", required = true) String userId,
						@QueryParameter(value = "projectId", required = true) String projectId,
						@QueryParameter("notifyBlamed") boolean notifyBlamed
	) throws IOException {
		Map<String, Map<String, String>> changedObjects = new HashMap<String, Map<String, String>>();

		try {
			testNames = request.getParameterValues("testNames"); // Not sure why I need to do this, but the auto-inject splits the string. So I leave it in the method signature to show it's required.
			Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
			User userToBlame;
			if ("{null}".equals(userId)) {
				userToBlame = null;
			} else {
				userToBlame = User.get(userId);
			}
			User currentUser = User.current();

			Status status = Status.NotAccepted;
			if (userToBlame == null) {
				status = Status.Unassigned;
			} else if (currentUser != null && currentUser.getId().equals(userToBlame.getId())) {
				status = Status.Accepted;
			}

			for (String testName : testNames) {
				Map<String, String> changedObject = new HashMap<String, String>();
				changedObjects.put(testName, changedObject);
				if (userToBlame != null) {
					changedObject.put("culprit", userToBlame.getId());
				} else {
					changedObject.put("culprit", "{null}");
				}
				changedObject.put("status", status.toString());

				blamer.setCulprit(testName, userToBlame);
				blamer.setStatus(testName, status);
			}

			if (notifyBlamed) {
				notifyBlamed(userToBlame, testNames, projectId);
			}
		} catch (MessagingException e) {
			LOGGER.severe("Couldn't send email\n" + ExceptionUtils.getFullStackTrace(e));
		} finally {
			JSONObject json = JSONObject.fromObject(changedObjects);
			json.write(response.getWriter());
		}
	}

	public void doUpdateStatus(StaplerRequest request, StaplerResponse response,
							   @QueryParameter(value = "testNames", required = true) String[] testNames,
							   @QueryParameter(value = "status", required = true) String statusStr,
							   @QueryParameter(value = "projectId", required = true) String projectId
	) throws IOException {
		Map<String, Map<String, String>> changedObjects = new HashMap<String, Map<String, String>>();
		try {
		testNames = request.getParameterValues("testNames"); // Not sure why I need to do this, but the auto-inject splits the string. So I leave it in the method signature to show it's required.
		Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
		Status status = Status.valueOf(statusStr);
		User currentUser = User.current();
		if (currentUser != null) {
			for (String testName : testNames) {
				User culprit = blamer.getCulprit(testName);
				if (culprit != null && currentUser.getId().equals(culprit.getId())) {
					Map<String, String> changedObject = new HashMap<String, String>();
					changedObjects.put(testName, changedObject);
					changedObject.put("status", status.toString());
					blamer.setStatus(testName, status);
				}
			}
		}
		} finally {
			JSONObject json = JSONObject.fromObject(changedObjects);
			json.write(response.getWriter());
		}
	}

	private void notifyBlamed(User blamedUser, String[] testNames, String projectId) throws MessagingException {
		InternetAddress emailForBlamedUser = getEmailForUser(blamedUser);
		if(emailForBlamedUser == null) {
			LOGGER.info("blamed user was null, not sending email");
			return;
		}

		if(testNames == null) {
			testNames = new String[0];
		}

		if(projectId == null) {
			projectId = "{Unknown Project}";
			LOGGER.info("projectID was null. This shouldn't be possible. Using " + projectId);
		}

		User currentUser = User.current();
		if(currentUser == null) {
			currentUser = User.getUnknown();
		}
		InternetAddress emailForCurrentUser = getEmailForUser(currentUser);

		if(emailForBlamedUser.equals(emailForCurrentUser)) {
			LOGGER.info(currentUser.getDisplayName() + " assigned tests to themself. Skipping email.");
			return;
		}

		String subject = generateSubject(currentUser, blamedUser, projectId);
		String content = generateContent(currentUser, blamedUser, testNames, projectId);

		MimeMessage msg = createEmailMessage(subject, content, emailForBlamedUser, emailForCurrentUser);
		Transport.send(msg);
	}

	private MimeMessage createEmailMessage(String subject, String content, InternetAddress toEmail, InternetAddress ccEmail) throws MessagingException {
		MimeMessage msg = new MimeMessage(Mailer.descriptor().createSession());

		String adminAddress = Mailer.descriptor().getAdminAddress();
		if(adminAddress != null && !adminAddress.isEmpty()) {
			msg.setFrom(new InternetAddress(adminAddress));
		}

		if(toEmail != null) {
			msg.setRecipients(Message.RecipientType.TO, new InternetAddress[] { toEmail } );
		}

		if(ccEmail != null) {
			msg.setReplyTo(new Address[]{ccEmail});
			msg.setRecipients(Message.RecipientType.CC, new InternetAddress[] { ccEmail } );
		}

		msg.setSentDate(new Date());
		msg.setSubject(subject);
		msg.setContent(content, "text/html");

		return msg;
	}

	private String generateSubject(User currentUser, User blamedUser, String projectId) {
		StringBuilder sb = new StringBuilder("Bespin Claim/Blame: ");

		sb.append(currentUser.getDisplayName()).append(" assigned ").append(blamedUser.getDisplayName()).append(" to tests on ").append(projectId);

		return sb.toString();
	}

	private String generateContent(User currentUser, User blamedUser, String[] testNames, String projectId) {
		StringBuilder sb = new StringBuilder("<html><body>");

		sb.append("<div>").append(blamedUser.getDisplayName()).append(" has been assigned to the following tests on ").append(projectId).append("</div>");

		sb.append("<ul>");
		if(testNames != null) {
			for (String testName : testNames) {
				sb.append("<li>").append(testName).append("</li>");
			}
		} else {
			sb.append("<li>").append("No Tests. Derp?").append("</li>");
		}
		sb.append("</ul>");

		return sb.append("</body></html>").toString();
	}


	private InternetAddress getEmailForUser(User user) throws AddressException {
		if(user == null) {
			return null;
		}

		if(user.getId().equals(User.getUnknown().getId())) {
			return null;
		}

		Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
		if(mailProperty == null) {
			return null;
		}

		String userEmailAddress = mailProperty.getAddress();
		if(userEmailAddress == null || userEmailAddress.isEmpty()) {
			return null;
		}

		return new InternetAddress(userEmailAddress);
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return "claimBlame";
	}
}
