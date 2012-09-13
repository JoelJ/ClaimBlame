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
 * To change this template use File | Settings | File Templates.
 */
public class CulpritAssignment extends UserProperty {
    public Map<String, TestAssignment> findAssignments() {
        Map<String, TestAssignment> userAssignments = new HashMap<String, TestAssignment>();
        Collection<String> jobNames = Jenkins.getInstance().getJobNames();
        for (String jobName : jobNames) {
            AbstractProject project = Project.findNearest(jobName);
            Run lastSuccessfulBuild = project.getLastSuccessfulBuild();
            while (lastSuccessfulBuild != null) {
                TestResultAction action = lastSuccessfulBuild.getAction(TestResultAction.class);
                if (action != null) {
                    List<CaseResult> failedTests = action.getFailedTests();
                    if (!failedTests.isEmpty()) {
                        for (CaseResult failedTest : failedTests) {
                            String testName = failedTest.getClassName() + "." + failedTest.getDisplayName();
                            FileSystemBlamer blamerForJob = (FileSystemBlamer) BlamerFactory.getBlamerForJob(project);
                            User culprit = blamerForJob.getCulprit(testName);
                            if (culprit != null && culprit.getId().equals(user.getId())) {
                                String url = failedTest.getUrl();
                                String buildUrl = lastSuccessfulBuild.getUrl();
                                Status testStatus = blamerForJob.getStatus(testName);
                                TestAssignment testAssignment = new TestAssignment(buildUrl+"testReport"+url, testName, testStatus);
                                userAssignments.put(testName, testAssignment);
                            }
                        }
                    }
                }
                lastSuccessfulBuild = lastSuccessfulBuild.getPreviousSuccessfulBuild();
            }
        }
        return userAssignments;
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
