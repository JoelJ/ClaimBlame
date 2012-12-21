package com.joelj.jenkins.claimblame;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.TransientViewActionFactory;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: josephbass
 * Date: 12/18/12
 * Time: 4:29 PM
 */
@Extension
public class ApiActionFactory extends TransientProjectActionFactory {

	@Override
	public Collection<? extends Action> createFor(AbstractProject abstractProject) {
		return Arrays.asList(new ApiAction(abstractProject));
	}
}
