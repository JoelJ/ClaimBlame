package com.joelj.jenkins.claimblame;

import java.io.Serializable;

/**
 * Represents a assignment for a test. Including the user it's assigned to and the status of the assignment.
 *
 * User: joeljohnson
 * Date: 5/26/12
 * Time: 12:30 AM
 */
public class Assignment implements Serializable {
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
