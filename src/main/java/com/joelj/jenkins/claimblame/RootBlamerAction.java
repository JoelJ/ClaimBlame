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
						@QueryParameter(value="testNames", required = true) List<String> testNames,
						@QueryParameter(value="userId", required = true) String userId,
						@QueryParameter(value="projectId", required = true) String projectId,
						@QueryParameter("notifyBlamed") boolean notifyBlamed
				) {
		Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
		User userToBlame = User.get(userId);
		User currentUser = User.current();

		Status status = Status.NotAccepted;
		if(currentUser.getId().equals(userToBlame.getId())) {
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
						@QueryParameter(value="testNames", required = true) List<String> testNames,
						@QueryParameter(value="status", required = true) String statusStr,
						@QueryParameter(value="projectId", required = true) String projectId
	) {
		Blamer blamer = BlamerFactory.getBlamerForJob(projectId);
		Status status = Status.valueOf(statusStr);
		for (String testName : testNames) {
			blamer.setStatus(testName, status);
		}
	}

	private void notifyBlamed(User user, List<String> testNames, String projectId) {
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
