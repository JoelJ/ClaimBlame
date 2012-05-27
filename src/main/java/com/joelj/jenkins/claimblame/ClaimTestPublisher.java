package com.joelj.jenkins.claimblame;

import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.junit.*;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.*;

/**
 * User: joeljohnson
 * Date: 4/11/12
 * Time: 7:32 PM
 */
public class ClaimTestPublisher extends TestDataPublisher {
	@DataBoundConstructor
	public ClaimTestPublisher() {

	}

	@Override
	public TestResultAction.Data getTestData(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, TestResult testResult) throws IOException, InterruptedException {
		resolveTests(build, testResult.getSuites());
		return new TestResultAction.Data() {
			@Override
			@SuppressWarnings("deprecation")
			public List<? extends TestAction> getTestAction(TestObject testObject) {
				Blamer blamer = BlamerFactory.getBlamerForJob(build.getProject());
				ImmutableList.Builder<TestAction> builder = ImmutableList.builder();
				if(testObject instanceof CaseResult) {
					builder.add(new BlameAction(((CaseResult) testObject).getFullName(), blamer, build.getUrl() + "testReport" + testObject.getUrl()));
				}
				return builder.build();
			}
		};
	}

	private void resolveTests(AbstractBuild<?, ?> build, Collection<SuiteResult> suites) {
		Blamer blamer = BlamerFactory.getBlamerForJob(build.getProject());

		User culprit = getRandomCulprit(build);

		for (SuiteResult suite : suites) {
			for (CaseResult caseResult : suite.getCases()) {
				if(caseResult.isPassed()) {
					blamer.setStatus(caseResult.getFullName(), Status.Fixed);
				} else if(caseResult.getAge() == 1) {
					//if culprit is null, it will remove any assignment
					blamer.setCulprit(caseResult.getFullName(), culprit);
				}
			}
		}
	}

	private User getRandomCulprit(AbstractBuild<?, ?> build) {
		User culprit = null;
		List<User> culprits = new ArrayList<User>(build.getCulprits());
		if(culprits.size() > 0) {
			int random = new Random().nextInt(culprits.size());
			culprit = culprits.get(random);
		}
		return culprit;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		@Override
		public String getDisplayName() {
			return "Enable claiming/blaming test results";
		}
	}
}
