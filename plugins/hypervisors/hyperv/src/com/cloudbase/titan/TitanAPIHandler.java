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
package com.cloudbase.titan;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.HashMap;

import com.cloud.agent.api.Answer;
import com.cloudbase.AsyncAnswer;
import com.cloudbase.serializer.Serializer;
import com.cloudbase.serializer.UrlSerializer;
import com.google.gson.Gson;

@SuppressWarnings("rawtypes")
public class TitanAPIHandler implements InvocationHandler {
	private Serializer remote;
	private Gson gson = Serializer.getGson();

	public static Object getTitanAPIInstance(String hostUrl) throws UnknownHostException, IOException {
		return getTitanAPIInstance(new UrlSerializer(hostUrl));
	}

	public static Object getTitanAPIInstance(Serializer communicator) throws UnknownHostException, IOException {
		Class[] interfaces = new Class[] { TitanAPI.class };
		return java.lang.reflect.Proxy.newProxyInstance(
				TitanAPI.class.getClassLoader(), interfaces,
				new TitanAPIHandler(communicator));
	}

	private TitanAPIHandler(Serializer remote) throws NumberFormatException, UnknownHostException, IOException {
		this.remote = remote;
	}

	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
		Object result = null;

		try {
			TitanAPIEnum cmd = TitanAPIEnum.getTitanCommand(m.getName());
			HashMap data = cmd.mapArgumentsForCommand(args);

			// refactor to a better format
			if (!cmd.isAsync()) {
				if (isType(m.getReturnType(), Boolean.class)) {
					Answer a = (Answer) remote
							.sendData(cmd, data, Answer.class);
					if (a == null)
						return false;
					return a.getResult();
				}

				Object obj = remote.sendData(cmd, data, m.getReturnType());
				if (obj == null)
					throw new TitanAPIException(
							"Null object returned from call.");

				return obj;
			}

			AsyncAnswer a = (AsyncAnswer) remote.sendData(cmd, data,
					AsyncAnswer.class);

			waitForAsyncJob(a);

			if (m.getReturnType().equals(Answer.class))
				return a;

			return gson.fromJson(a.getObj(), m.getReturnType());

		} catch (Exception e) {
			throw new TitanAPIException(e);
		}
	}

	private static boolean isType(Class a, Class b) {
		if (a.equals(b))
			return true;

		Class temp = a.getSuperclass();

		while (!temp.equals(Object.class)) {
			if (temp.equals(b))
				return false;

			temp = temp.getSuperclass();
		}

		return false;
	}

	private AsyncAnswer waitForAsyncJob(AsyncAnswer a) throws IOException {
		if (a.getJobId() == null || a.getJobId().equals(""))
			return a;

		AsyncAnswer ret = a;
		String job = a.getJobId();

		int totalTime = 0;
		while (ret.getWait() > 0 && totalTime < 60) {
			try {
				Thread.sleep(ret.getWait() * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			totalTime += ret.getWait();
			ret = (AsyncAnswer) remote.sendSimple(TitanAPIEnum.CheckAsyncJob,
					AsyncAnswer.class, job);

		}

		return ret;
	}

}
