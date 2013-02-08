package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.User;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.*;

/**
 * User: Joel Johnson
 * Date: 2/7/13
 * Time: 12:37 PM
 */
@Extension
public class RootBlamerAction implements RootAction {
	public void doBlame(StaplerRequest request, StaplerResponse response,
						@QueryParameter(value="testNames", required = true) String[] testNames,
						@QueryParameter(value="userId", required = true) String userId,
						@QueryParameter(value="projectId", required = true) String projectId,
						@QueryParameter("notifyBlamed") boolean notifyBlamed
				) {
		testNames = request.getParameterValues("testNames"); // Not sure why I need to do this, but the auto-inject splits the string. So I leave it in the method signature to show it's required.
		Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
		User userToBlame;
		if("{null}".equals(userId)) {
			userToBlame = null;
		} else {
			userToBlame = User.get(userId);
		}
		User currentUser = User.current();

		Status status = Status.NotAccepted;
		if(userToBlame == null) {
			status = Status.Unassigned;
		} else if(currentUser != null && currentUser.getId().equals(userToBlame.getId())) {
			status = Status.Accepted;
		}

		for (String testName : testNames) {
			blamer.setCulprit(testName, userToBlame);
			blamer.setStatus(testName, status);
		}

		if(notifyBlamed) {
			notifyBlamed(userToBlame, testNames, projectId);
		}
	}

	public void doUpdateStatus(StaplerRequest request, StaplerResponse response,
						@QueryParameter(value="testNames", required = true) String[] testNames,
						@QueryParameter(value="status", required = true) String statusStr,
						@QueryParameter(value="projectId", required = true) String projectId
	) {
		testNames = request.getParameterValues("testNames"); // Not sure why I need to do this, but the auto-inject splits the string. So I leave it in the method signature to show it's required.
		Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
		Status status = Status.valueOf(statusStr);
		for (String testName : testNames) {
			blamer.setStatus(testName, status);
		}
	}

	private void notifyBlamed(User user, String[] testNames, String projectId) {
		//TODO: I'll do this later. I just really need to get the basic functionality working.
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