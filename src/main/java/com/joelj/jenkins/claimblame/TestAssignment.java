package com.joelj.jenkins.claimblame;

/**
 * Created with IntelliJ IDEA.
 * User: brianmondido
 * Date: 8/15/12
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAssignment {
    private String testUrl;
    private String testName;
    private Status testStatus;
    public TestAssignment(String testUrl, String testName, Status status) {
        setTestUrl(testUrl);
        setTestName(testName);
        setTestStatus(status);
    }

    public String getTestUrl() {
        return testUrl;
    }

    private void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Status getTestStatus() {
        return testStatus;
    }

    public void setTestStatus(Status testStatus) {
        this.testStatus = testStatus;
    }
}
