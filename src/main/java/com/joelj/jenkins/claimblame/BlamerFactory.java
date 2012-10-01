package com.joelj.jenkins.claimblame;

import hudson.model.Job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		return getBlamerForJob(job.getFullName());
	}

	public static Blamer getBlamerForJob(String jobName) {
		Blamer result;
		if(!blamers.containsKey(jobName)) {
			FileSystemBlamer fileSystemBlamer = new FileSystemBlamer(jobName);
			fileSystemBlamer.load();
			blamers.put(jobName, fileSystemBlamer);
			result = fileSystemBlamer;
		} else {
			result = blamers.get(jobName);
		}
		return result;
	}

	public static Set<String> getTrackedJobs() {
		return blamers.keySet();
	}
}
