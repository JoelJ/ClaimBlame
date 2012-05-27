package com.joelj.jenkins.claimblame;

import hudson.model.Hudson;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * User: joeljohnson
 * Date: 4/11/12
 * Time: 7:49 PM
 */
public class BlameAction extends TestAction {
	private final String testName;
	private final Blamer blamer;
	private final String testUrl;

	@SuppressWarnings("deprecation")
	public BlameAction(String testName, Blamer blamer, String testUrl) {
		this.testName = testName;
		this.blamer = blamer;
		this.testUrl = testUrl;
	}

	public void doBlame(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
		String userID = request.getParameter("userID");
		if (userID != null && !userID.equals("{null}")) {
			User userToBlame = User.get(userID, false); //User.get returns a dummy object if it doesn't exist
			if (User.getAll().contains(userToBlame)) {
				blamer.setCulprit(testName, userToBlame);
			}
		} else {
			blamer.setCulprit(testName, null);
		}
		writeCulpritStatusToStream(response.getOutputStream());
	}

	public void doStatus(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
		String statusString = request.getParameter("status");
		if (statusString != null) {
			Status status = Status.valueOf(statusString);
			blamer.setStatus(testName, status);
		} else {
			blamer.setStatus(testName, Status.NotAccepted);
		}
		writeCulpritStatusToStream(response.getOutputStream());
	}

	private void writeCulpritStatusToStream(ServletOutputStream outputStream) throws IOException {
		User culprit = getCulprit();
		outputStream.print("{");
		outputStream.print("\"culprit\":" + (culprit == null ? "null" : ("\"" + culprit.getId() + "\"")) + ",");
		outputStream.print("\"status\":\"" + getStatus() + "\",");
		outputStream.print("\"isYou\":" + (culprit != null && culprit.equals(User.current())) + ",");
		outputStream.print("}");
		outputStream.flush();
		outputStream.close();
	}

	public String getUrl() {
		return Hudson.getInstance().getRootUrl() + this.testUrl;
	}

	public User getCulprit() {
		return blamer.getCulprit(testName);
	}

	public Status getStatus() {
		return blamer.getStatus(testName);
	}

	public List<User> getUsers() {
		List<User> allUsers = new LinkedList<User>(User.getAll());
		Collections.sort(allUsers);
		return allUsers;
	}

	public User getCurrentUser() {
		return User.current();
	}

	public String getTestName() {
		return testName;
	}

	public Blamer getBlamer() {
		return blamer;
	}

	public String getTestUrl() {
		return testUrl;
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return "blame";
	}
}
