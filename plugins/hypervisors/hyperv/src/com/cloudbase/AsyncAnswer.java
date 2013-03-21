package com.cloudbase;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class AsyncAnswer extends Answer {
	private String obj;
	private String jobId;

	public String getObj() {
		return obj;
	}

	public void setObj(String obj) {
		this.obj = obj;
	}

	public AsyncAnswer() {
		super();
	}

	public AsyncAnswer(Command command, boolean success, String details) {
		super(command, success, details);
	}

	public AsyncAnswer(Command command, Exception e) {
		super(command, e);
	}

	public AsyncAnswer(Command command) {
		super(command);
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobId() {
		return jobId;
	}

}
