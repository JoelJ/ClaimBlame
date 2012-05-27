package com.joelj.jenkins.claimblame;

import hudson.model.Job;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Temporary location to get the blamer for a job.
 * TODO: Add extension points to allow others to create different blamers.
 * TODO: 	For example, have it sync with bug trackers rather than the filesystem.
 * User: joeljohnson
 * Date: 5/26/12
 * Time: 12:22 AM
 */
public class BlamerFactory {
	private static Map<String, Blamer> blamers = new WeakHashMap<String, Blamer>();

	public static Blamer getBlamerForJob(Job job) {
		String fullName = job.getFullName();
		Blamer result;
		if(!blamers.containsKey(fullName)) {
			FileSystemBlamer fileSystemBlamer = new FileSystemBlamer(fullName);
			fileSystemBlamer.load();
			blamers.put(fullName, fileSystemBlamer);
			result = fileSystemBlamer;
		} else {
			result = blamers.get(fullName);
		}
		return result;
	}
}
