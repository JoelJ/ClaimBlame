package com.joelj.jenkins.claimblame;

import hudson.model.Hudson;
import hudson.model.User;
import hudson.tasks.junit.TestAction;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.*;

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
				User current=User.current();
				if(current!=null && userToBlame.getId().equals(current.getId())){
					blamer.setStatus(testName,Status.Accepted);
				}
            }
        } else {
            blamer.setCulprit(testName, null);
        }
        writeCulpritStatusToStream(response.getOutputStream());
    }

    public void doBulkBlame(StaplerRequest request, StaplerResponse response) throws IOException {
        String userID = request.getParameter("userID");
        String[] testNames = request.getParameterValues("testNames");
        if (userID != null && !userID.equals("{null}")) {
            User userToBlame = User.get(userID, false);
            if (User.getAll().contains(userToBlame)) {
                for (String name : testNames) {

                    blamer.setCulprit(name, userToBlame);
					User current=User.current();
					if(current!=null && userToBlame.getId().equals(current.getId())){
						blamer.setStatus(name,Status.Accepted);
					}
                }
            }
        } else {
            for (String name : testNames) {
                blamer.setCulprit(name, null);
            }
        }
		response.setContentType("application/json");
        writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
    }

	public void doBulkDone(StaplerRequest request, StaplerResponse response) throws IOException {
		String userID = request.getParameter("userID");
		String[] testNames = request.getParameterValues("testNames");
		if (userID != null && !userID.equals("{null}")) {
			User userToBlame = User.get(userID, false);
			if (User.getAll().contains(userToBlame)) {
				for (String name : testNames) {
					User current=User.current();
					if(current!=null && userToBlame.getId().equals(current.getId())){
						blamer.setStatus(name,Status.Done);
					}
				}
			}
		} else {
			for (String name : testNames) {
				blamer.setCulprit(name, null);
			}
		}
		response.setContentType("application/json");
		writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
	}


    public void doStatus(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String statusString = request.getParameter("status");
        if (statusString != null) {
            Status status = Status.valueOf(statusString);
            blamer.setStatus(testName, status);
        } else {
            blamer.setStatus(testName, Status.NotAccepted);
        }
        response.setContentType("application/json");
        writeCulpritStatusToStream(response.getOutputStream());
    }

	public void doBulkStatus(StaplerRequest request, StaplerResponse response) throws IOException {
		String[] testNames=request.getParameterValues("testNames");
		String statusValue=request.getParameter("status");

		for (String name : testNames) {
			blamer.setStatus(name, Status.valueOf(statusValue));
		}
		response.setContentType("application/json");
		writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
	}

    private void writeBulkCulpritStatusToStream(ServletOutputStream outputStream, String[] testNames) throws IOException {
        JSONObject jsonObject = new JSONObject();
        for (String name : testNames) {
            Map<String, String> testData = new HashMap<String, String>();
            User culprit = getCulprit(name);
            testData.put("culprit", culprit == null ? "null" : culprit.getId());
            testData.put("status", getStatus(name).name());
            testData.put("isYou", String.valueOf((culprit != null && culprit.equals(User.current()))));
            jsonObject.put(name, testData);
        }
        outputStream.print(jsonObject.toString());
        outputStream.flush();
        outputStream.close();
    }

    private void writeCulpritStatusToStream(ServletOutputStream outputStream) throws IOException {
        User culprit = getCulprit(testName);
        outputStream.print("{");
        outputStream.print("\"culprit\":" + (culprit == null ? "null" : ("\"" + culprit.getId() + "\"")) + ",");
        outputStream.print("\"status\":\"" + getStatus(testName) + "\",");
        outputStream.print("\"isYou\":" + (culprit != null && culprit.equals(User.current())) + ",");
        outputStream.print("}");
        outputStream.flush();
        outputStream.close();
    }

    public String getUrl() {
        return Hudson.getInstance().getRootUrl() + this.testUrl;
    }

    public User getCulprit(String testName) {
        return blamer.getCulprit(testName);
    }

    public Status getStatus(String testName) {
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
