package com.joelj.jenkins.claimblame;

/**
 * Created with IntelliJ IDEA.
 * User: joeljohnson
 * Date: 5/26/12
 * Time: 12:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class Assignment {
	private final String userId;
	private Status status;

	public Assignment(String userId) {
		this.userId = userId;
		this.status = Status.NotAccepted;
	}

	public String getUserId() {
		return userId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
