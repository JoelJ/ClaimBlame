package com.joelj.jenkins.claimblame;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.model.*;
import hudson.model.listeners.SaveableListener;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: joeljohnson
 * Date: 4/11/12
 * Time: 7:56 PM
 */
public class FileSystemBlamer implements Blamer, Saveable {
	private static final XStream XSTREAM = new XStream2();
	private static final Logger LOGGER = Logger.getLogger(FileSystemBlamer.class.getName());
	private transient boolean loaded = false;

	private String jobName;
	private Map<String, Assignment> culprits;

	FileSystemBlamer(String jobName) {
		this.jobName = jobName;
		this.culprits = new HashMap<String, Assignment>();
	}

	@Override
	public void setCulprit(String testName, User user) {
		if(user != null) {
			culprits.put(testName, new Assignment(user.getId()));
		} else {
			culprits.remove(testName);
		}
		try {
			save();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to save.", e);
		}
	}

	@Override
	public User getCulprit(String testName) {
		if(!loaded) {
			load();
		}
		if(culprits.containsKey(testName)) {
			return User.get(culprits.get(testName).getUserId(), false);
		} else {
			return null;
		}
	}

	@Override
	public void setStatus(String testName, Status status) {
		if(culprits.containsKey(testName)) {
			Assignment assignment = culprits.get(testName);
			assignment.setStatus(status);

			try {
				save();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Failed to save.", e);
			}
		}
	}

	@Override
	public Status getStatus(String testName) {
		if(!loaded) {
			load();
		}
		if(culprits.containsKey(testName)) {
			return culprits.get(testName).getStatus();
		} else {
			return Status.Unassigned;
		}
	}

	@Override
	public synchronized void save() throws IOException {
		getConfigFile().write(this);
		SaveableListener.fireOnChange(this, getConfigFile());
	}

	public synchronized void load() {
		XmlFile config = getConfigFile();
		if (config.exists()) {
			try {
				config.unmarshal(this);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Failed to load " + config, e);
			}
		} else {
			try {
				save();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Failed to load " + config, e);
			}
		}
		loaded = true;
	}

	protected final XmlFile getConfigFile() {
		return new XmlFile(XSTREAM, new File(getRootDir(), jobName + "/config.xml"));
	}

	protected static File getRootDir() {
		return new File(Hudson.getInstance().getRootDir(), "claimBlame");
	}
}
