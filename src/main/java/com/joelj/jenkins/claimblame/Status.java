package com.joelj.jenkins.claimblame;

/**
 * User: joeljohnson
 * Date: 5/26/12
 * Time: 12:29 AM
 */
public enum Status {
	/**
	 * Test is not currently assigned to anyone
	 */
	Unassigned,

	/**
	 * Test is assigned, but the user hasn't explicitly accepted responsibility for the test
	 */
	NotAccepted,

	/**
	 * Test is assigned, and the user has explicitly accepted responsibility for the test
	 */
	Accepted,

	/**
	 * The user has marked the failing test to be fixed, so it should pass in an upcomming revision
	 */
	Done,

	/**
	 * The test has run and passed and is no longer failing.
	 */
	Fixed
}
