package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.*;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
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
			Set<String> relevantTests = getRelevantTests(tests, jobName);
			for (String fullyQualifiedTestName : relevantTests) {
				User culprit = blamer.getCulprit(fullyQualifiedTestName);
				if (culprit != null && culprit.getId().equals(user.getId())) {
					String url = "job/" + jobName + "/lastSuccessfulBuild/testReport/" + getTestUrl(fullyQualifiedTestName);
					TestAssignment testAssignment = new TestAssignment(url, fullyQualifiedTestName, blamer.getStatus(fullyQualifiedTestName));
					userAssignments.put(fullyQualifiedTestName, testAssignment);
				}
			}
		}
		return userAssignments;
	}

	private Set<String> getRelevantTests(Set<String> tests, String jobName) {
		Set<String> relevantTests = new HashSet<String>();
		AbstractProject job = Jenkins.getInstance().getItem(jobName, Jenkins.getInstance(), AbstractProject.class);
		if (job != null) {
			Run build = job.getLastSuccessfulBuild();
			AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
			if (testResultAction != null) {
				List<CaseResult> failedTests = testResultAction.getFailedTests();
				if (failedTests != null) {
					for (CaseResult failedTest : failedTests) {
						String testName = failedTest.getFullName();
						if (tests.contains(testName)) {
							relevantTests.add(testName);
						}
					}
				}
			}
		}
		return relevantTests;
	}

	private String getTestName(String fullyQualifiedTestName) {
		int lastDotIndex = fullyQualifiedTestName.lastIndexOf(".");
		return fullyQualifiedTestName.substring(lastDotIndex + 1);

	}

	private String getTestUrl(String fullyQualifiedTestName) {
		int lastDotIndex = fullyQualifiedTestName.lastIndexOf(".");
		String methodName = getTestName(fullyQualifiedTestName);
		int nextDotIndex = fullyQualifiedTestName.lastIndexOf(".", lastDotIndex - 1);
		String className;
		String packageName;
		if (nextDotIndex > -1) {
			className = fullyQualifiedTestName.substring(nextDotIndex+1, lastDotIndex);
			packageName = fullyQualifiedTestName.substring(0, nextDotIndex);
		} else {
			className = fullyQualifiedTestName.substring(0, lastDotIndex);
			packageName = "(root)";
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
