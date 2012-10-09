package com.joelj.jenkins.claimblame;

import hudson.model.*;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResultAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: joeljohnson
 * Date: 4/11/12
 * Time: 7:49 PM
 */
public class BlameAction extends TestAction {
	private final String testName;
	private final Blamer blamer;
	private final String testUrl;
	private final String buildId;

	@SuppressWarnings("deprecation")
	public BlameAction(String buildId,String testName, Blamer blamer, String testUrl) {
		this.testName = testName;
		this.blamer = blamer;
		this.testUrl = testUrl;
		this.buildId=buildId;
	}

    public void doBlame(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, MessagingException {
        String userID = request.getParameter("userID");
        if (userID != null && !userID.equals("{null}")) {
            User userToBlame = User.get(userID, false); //User.get returns a dummy object if it doesn't exist
            if (User.getAll().contains(userToBlame)) {
                blamer.setCulprit(testName, userToBlame);
				User current=User.current();
				if(current!=null && userToBlame.getId().equals(current.getId())){
					blamer.setStatus(testName,Status.Accepted);
					mailCommitters(current,testName);
				} else if(current!=null && !userToBlame.getId().equals(current.getId())){
					mailFailures(userToBlame,Arrays.asList(testName));
				}
            }
        } else {
            blamer.setCulprit(testName, null);
        }
        writeCulpritStatusToStream(response.getOutputStream());
    }

	private void mailCommitters(User current, String... testNames) throws MessagingException {
		//TODO mail all the committers of this build, User current has accepted the following tests
		//find committers of a build
		//mail each of them.

		Run<?, ?> build = getBuild();
		if(build!=null){
			Set<String> committers = ClaimTestPublisher.findCommitters((AbstractBuild) build);
			if(committers.size()>0){
				for (String committerID : committers) {
					User user = User.get(committerID);
					if(User.getAll().contains(user)){
						Mailer.UserProperty userProperty = user.getProperty(Mailer.UserProperty.class);
						if(userProperty==null){
							continue;
						}
						String email = userProperty.getAddress();
						if(email==null || email.isEmpty()){
							continue;
						}
						MimeMessage msg=new MimeMessage(Mailer.descriptor().createSession());
						if(current!=null){
							Mailer.UserProperty currentUserProperty = current.getProperty(Mailer.UserProperty.class);
							if(currentUserProperty != null) {
								InternetAddress emailAddress = new InternetAddress(userProperty.getAddress());
								msg.setReplyTo(new Address[]{emailAddress});
							}
						}
						msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
						StringBuilder urlBuilder=new StringBuilder();
						urlBuilder.append(Jenkins.getInstance().getRootUrl()).append(build.getUrl());
						String url=urlBuilder.toString();
						msg.setSentDate(new Date());
						msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
						msg.setSubject("Build "+ buildId +":Accepted Failures");
						StringBuilder messageBuilder=new StringBuilder();
						assert current != null;
						messageBuilder.append("User ")
								.append(current.getFullName())
								.append(" has accepted the following tests of your <a href=\"")
										.append(url)
										.append("\">Build ")
										.append(buildId)
										.append("</a>");
						for (String name : testNames) {
							messageBuilder.append("<br/>")
									.append(name);
						}
						msg.setContent(messageBuilder.toString(),"text/html");
						Transport.send(msg);
					}
				}
			}
		}
	}

	public void doBulkBlame(StaplerRequest request, StaplerResponse response) throws IOException, MessagingException {
		boolean doMailCommitters=false;
        String userID = request.getParameter("userID");
        String[] testNames = request.getParameterValues("testNames");
        if (userID != null && !userID.equals("{null}")) {
            User userToBlame = User.get(userID, false);
            if (testNames!=null && User.getAll().contains(userToBlame)) {
                for (String name : testNames) {
                    blamer.setCulprit(name, userToBlame);
					User current=User.current();
					if(current!=null && userToBlame.getId().equals(current.getId())){
						blamer.setStatus(name,Status.Accepted);
						doMailCommitters=true;
					}

                }
				if(doMailCommitters){
					mailCommitters(User.current(), testNames);
				}
				mailFailures(userToBlame,Arrays.asList(testNames));
            }
        } else {
			if(testNames!=null){
				for (String name : testNames) {
					blamer.setCulprit(name, null);
				}
			}
        }
		response.setContentType("application/json");
        writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
    }

	private void mailFailures(User userToBlame, List<String> testNames) throws MessagingException {
		User currentUser = User.current();
		if(userToBlame==null || (currentUser !=null && userToBlame.getId().equals(currentUser.getId()))){
			return;
		}

		ArrayList<String> testsToMail = getTestsToMail(userToBlame, testNames);

		Mailer.UserProperty userToBlameProperty = userToBlame.getProperty(Mailer.UserProperty.class);
		if(userToBlameProperty == null) {
			return;
		}

		String email = userToBlameProperty.getAddress();
		if(email == null || email.isEmpty()) {
			return; //no one to email!
		}

		if(testsToMail.size()>0){
			Run<?, ?> build = getBuild();
			MimeMessage msg=new MimeMessage(Mailer.descriptor().createSession());
			if(currentUser!=null){
				Mailer.UserProperty userProperty = currentUser.getProperty(Mailer.UserProperty.class);
				if(userProperty != null) {
					InternetAddress emailAddress = new InternetAddress(userProperty.getAddress());
					msg.setReplyTo(new Address[]{emailAddress});
				}
			}
			msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
			StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append(Jenkins.getInstance().getRootUrl());
			urlBuilder.append(build.getUrl());
			String url=urlBuilder.toString();
			msg.setSentDate(new Date());
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
			msg.setSubject("Build " + build.getId() + ":Pending Failure Acceptance");
			StringBuilder messageBuilder=new StringBuilder();
			messageBuilder.append("You've been assigned to the following Failures on <a href=\"")
					.append(url)
					.append("\">Build ")
					.append(buildId)
					.append("</a>");
			for (String test : testsToMail) {
				messageBuilder.append("<br/>")
						.append(test);
			}
			messageBuilder.append("<br/>");
			messageBuilder.append("To View your assigned tests, look at your <a href=\"")
					.append(Jenkins.getInstance().getRootUrl())
					.append("user/")
					.append(userToBlame.getId())
					.append("/\">User Page!</a>");
			msg.setContent(messageBuilder.toString(), "text/html");
			Transport.send(msg);
		}
	}

	private ArrayList<String> getTestsToMail(User userToBlame, List<String> testNames) {
		ArrayList<String> testsToMail = new ArrayList<String>();
		if(userToBlame!=null && testNames!=null){
			Run<?, ?> build = getBuild();
			if(build!=null){
				TestResultAction action = build.getAction(TestResultAction.class);
				if(action!=null){
					List<CaseResult> failedTests = action.getFailedTests();
					for (CaseResult failedTest : failedTests) {
						if(testNames.contains(failedTest.getFullName())){
							testsToMail.add(failedTest.getFullName());
						}
					}
				}
			}
		}
		return testsToMail;
	}

	public void doBulkDone(StaplerRequest request, StaplerResponse response) throws IOException {
		String userID = request.getParameter("userID");
		String[] testNames = request.getParameterValues("testNames");
		if (userID != null && !userID.equals("{null}")) {
			User userToBlame = User.get(userID, false);
			if (testNames!=null && User.getAll().contains(userToBlame)) {
				for (String name : testNames) {
					User current=User.current();
					if(current!=null && userToBlame.getId().equals(current.getId())){
						blamer.setStatus(name,Status.Done);
					}
				}
			}
		} else {
			if(testNames!=null){
				for (String name : testNames) {
					blamer.setCulprit(name, null);
				}
			}
		}
		response.setContentType("application/json");
		writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
	}

    public void doStatus(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String statusString = request.getParameter("status");
        if (statusString != null) {
            Status status = Status.valueOf(statusString);
            blamer.setStatus(testName, status);
        } else {
            blamer.setStatus(testName, Status.NotAccepted);
        }
        response.setContentType("application/json");
        writeCulpritStatusToStream(response.getOutputStream());
    }

	public void doBulkStatus(StaplerRequest request, StaplerResponse response) throws IOException {
		String[] testNames=request.getParameterValues("testNames");
		String statusValue=request.getParameter("status");
		if(testNames!=null){
			for (String name : testNames) {
				blamer.setStatus(name, Status.valueOf(statusValue));
			}
		}
		response.setContentType("application/json");
		writeBulkCulpritStatusToStream(response.getOutputStream(), testNames);
	}

    private void writeBulkCulpritStatusToStream(ServletOutputStream outputStream, String[] testNames) throws IOException {
        JSONObject jsonObject = new JSONObject();
		if(testNames!=null){
			for (String name : testNames) {
				Map<String, String> testData = new HashMap<String, String>();
				User culprit = getCulprit(name);
				testData.put("culprit", culprit == null ? "null" : culprit.getId());
				testData.put("status", getStatus(name).name());
				testData.put("isYou", String.valueOf((culprit != null && culprit.equals(User.current()))));
				jsonObject.put(name, testData);
			}
		}
        outputStream.print(jsonObject.toString());
        outputStream.flush();
        outputStream.close();
    }

    private void writeCulpritStatusToStream(ServletOutputStream outputStream) throws IOException {
        User culprit = getCulprit(testName);
        outputStream.print("{");
        outputStream.print("\"culprit\":" + (culprit == null ? "null" : ("\"" + culprit.getId() + "\"")) + ",");
        outputStream.print("\"status\":\"" + getStatus(testName) + "\",");
        outputStream.print("\"isYou\":" + (culprit != null && culprit.equals(User.current())) + ",");
        outputStream.print("}");
        outputStream.flush();
        outputStream.close();
    }

    public String getUrl() {
        return this.testUrl;
    }

    public User getCulprit(String testName) {
        return blamer.getCulprit(testName);
    }

    public Status getStatus(String testName) {
        return blamer.getStatus(testName);
    }

    public List<User> getUsers() {
        List<User> allUsers = new LinkedList<User>(User.getAll());
        Collections.sort(allUsers);
        return allUsers;
    }

    public User getCurrentUser() {
        return User.current();
    }

    public String getTestName() {
        return testName;
    }

    public Blamer getBlamer() {
        return blamer;
    }

    public String getTestUrl() {
        return testUrl;
    }

	public Run<?, ?> getBuild(){
		return buildId!=null ? Build.fromExternalizableId(buildId): null;
	}

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "blame";
    }
}
