// Copyright 2013 Cloudbase Solutions Srl
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
