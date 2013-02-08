package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.User;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.*;

/**
 * User: Joel Johnson
 * Date: 2/7/13
 * Time: 12:37 PM
 */
@Extension
public class RootBlamerAction implements RootAction {
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
