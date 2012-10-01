package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.*;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: brianmondido
 * Date: 8/14/12
 * Time: 1:51 PM
 */
public class CulpritAssignment extends UserProperty {
    public Map<String, TestAssignment> findAssignments() {
		Map<String, TestAssignment> userAssignments = new HashMap<String, TestAssignment>();
		for (String jobName : BlamerFactory.getTrackedJobs()) {
			Blamer blamer = BlamerFactory.getBlamerForJob(jobName);
			Set<String> tests = blamer.getTests();
			for (String testName : tests) {
				User culprit = blamer.getCulprit(testName);
				if (culprit != null && culprit.getId().equals(user.getId())) {
					String rootUrl = Jenkins.getInstance().getRootUrl();
					if (rootUrl == null) {
						rootUrl = "/";
					}
					String url = rootUrl + "job/" + jobName + "/lastSuccessfulBuild/testReport" + getTestUrl(testName);
					userAssignments.put(testName, new TestAssignment(url, testName, blamer.getStatus(testName)));
				}
			}
		}
        return userAssignments;
    }

	private String getTestUrl(String testName) {
		int lastDotIndex = testName.lastIndexOf(".");
		String methodName = testName.substring(lastDotIndex);
		int nextDotIndex = testName.lastIndexOf(".", lastDotIndex);
		String className;
		String packageName;
		if (nextDotIndex > -1) {
			className = testName.substring(nextDotIndex,lastDotIndex);
			packageName = testName.substring(0,nextDotIndex);
		} else {
			className = testName.substring(0, lastDotIndex);
			packageName="";
		}
		return packageName + "/" + className + "/" + methodName;
	}

	public List<User> getUsers() {
        List<User> allUsers = new LinkedList<User>(User.getAll());
        Collections.sort(allUsers);
        return allUsers;
    }

    @Extension
    public static final class DescriptorImpl extends UserPropertyDescriptor {
        public String getDisplayName() {
            return "Culprit Assignments";
        }

        public CulpritAssignment newInstance(User user) {
            return new CulpritAssignment();
        }
    }

    public List<String> getStatusValues() {
        List<String> statusValues = new ArrayList<String>();
        for (Status status : Status.values()) {
            if (status != Status.Fixed && status != Status.Unassigned)
                statusValues.add(status.toString());
        }
        return statusValues;
    }
}
