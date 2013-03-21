package com.joelj.jenkins.claimblame;

import hudson.model.Job;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary location to get the blamer for a job.
 * TODO: Add extension points to allow others to create different blamers.
 * TODO: 	For example, have it sync with bug trackers rather than the filesystem.
 * User: joeljohnson
 * Date: 5/26/12
 * Time: 12:22 AM
 */
public class BlamerFactory {
	private static Class<? extends Blamer> blamerClass = FileSystemBlamer.class; //TODO: make this configurable

	private static Map<String, Blamer> blamers = new ConcurrentHashMap<String, Blamer>();

	public static Blamer getBlamerForJob(Job job) {
		return getBlamerForJob(job.getFullName());
	}

	public static Blamer getBlamerForJob(String jobName) {
		Blamer result;
		if(!blamers.containsKey(jobName)) {
			try {
				Blamer blamer = blamerClass.newInstance();
				blamer.setJobName(jobName);
				blamer.load();
				blamers.put(jobName, blamer);
				result = blamer;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			result = blamers.get(jobName);
		}
		return result;
	}

	public static Set<String> getTrackedJobs() {
		Method getTrackedJobsMethod = null;
		try {
			getTrackedJobsMethod = blamerClass.getMethod("getTrackedJobs");
		} catch (NoSuchMethodException ignore) {
			//FIXME: this is gross.
		}

		if(getTrackedJobsMethod != null && Set.class.isAssignableFrom(getTrackedJobsMethod.getReturnType())) {
			getTrackedJobsMethod.setAccessible(true);
			Object result;
			try {
				result = getTrackedJobsMethod.invoke(null);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			if(result instanceof Set) {
				//noinspection unchecked
				return (Set<String>)result;
			}
			throw new RuntimeException("Unable to find tracked jobs. Returned an invalid value.");
		} else {
			return blamers.keySet();
		}
	}
}
