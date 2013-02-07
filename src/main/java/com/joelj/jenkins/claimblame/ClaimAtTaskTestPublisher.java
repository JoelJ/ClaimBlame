package com.joelj.jenkins.claimblame;

import com.attask.jenkins.testreport.*;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.User;

import java.io.IOException;
import java.util.Collection;

/**
 * User: Joel Johnson
 * Date: 2/7/13
 * Time: 11:17 AM
 */
@Extension
public class ClaimAtTaskTestPublisher extends TestDataPublisher {
	private transient User culprit;
	private transient Status status;
	private transient String testName;

	public User getCulprit() {
		return culprit;
	}

	public Status getStatus() {
		return status;
	}

	public Collection<User> getUsers() {
		return User.getAll();
	}

	public User getCurrentUser() {
		return User.current();
	}

	@Override
	public String getDisplayName() {
		return "ClaimBlame";
	}

	@Override
	public boolean before(AbstractBuild<?, ?> abstractBuild, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return true;
	}

	@Override
	public boolean each(AbstractBuild<?, ?> abstractBuild, TestResult testResult) throws IOException, InterruptedException {
		String key = abstractBuild.getProject().getName();
		String uniquifier = testResult.getUniquifier();
		if(uniquifier != null) {
			key = key + "." + uniquifier;
		}

		Blamer blamer = BlamerFactory.getBlamerForJob(key);

		testName = testResult.getName();
		culprit = blamer.getCulprit(testName);
		status = blamer.getStatus(testName);

		return true;
	}

	@Override
	public boolean after(AbstractBuild<?, ?> abstractBuild, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return false;
	}
}
