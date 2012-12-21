package com.joelj.jenkins.claimblame;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.User;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: josephbass
 * Date: 12/18/12
 * Time: 4:32 PM
 */
public class ApiAction implements Action {
	private final AbstractProject project;

	public ApiAction(AbstractProject project) {
		this.project = project;
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return "claimblame";
	}

	public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
		Map<String, String> assignments = new HashMap<String, String>();

		Blamer blamerForJob = BlamerFactory.getBlamerForJob(this.project);
		Run lastSuccessfulBuild = project.getLastSuccessfulBuild();
		AbstractTestResultAction action = lastSuccessfulBuild.getAction(TestResultAction.class);
		List<CaseResult> failedTests = action.getFailedTests();
		for (CaseResult failedTest : failedTests) {
			User culprit = blamerForJob.getCulprit(failedTest.getFullName());
			if (culprit != null) {
				String testName = failedTest.getFullName() + "()";
				assignments.put(testName, culprit.getId());
			}
		}
		response.getWriter().print(JSONObject.fromObject(assignments).toString(1));

	}
}
