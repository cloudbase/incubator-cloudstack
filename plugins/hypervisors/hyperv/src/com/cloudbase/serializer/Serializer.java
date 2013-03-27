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
package com.cloudbase.serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.bouncycastle.util.IPAddress;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.hypervisor.mo.HypervisorHostResourceSummary;
import com.cloud.vm.VirtualMachine;
import com.cloudbase.gson.AnswerTypeAdapter;
import com.cloudbase.gson.OtherTypeAdapter;
import com.cloudbase.titan.TitanAPIEnum;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("rawtypes")
public abstract class Serializer {
	protected static Logger logger = Logger.getLogger(Serializer.class);
	private static int port = 8000;
	private static String URI = "/Cloudbase/Titan/Service/";
	private String host;
	protected Gson gson;

	public Serializer(String host) {
		super();
		this.host = host;

		if (IPAddress.isValid(host))
			this.host = "http://" + host;

		if (!host.contains(":")) {
			int slash = this.host.indexOf("/", 8);
			if (slash == -1)
				this.host = this.host + ":" + port + URI;
			else
				this.host = this.host.substring(0, slash) + ":" + port
						+ this.host.substring(slash, this.host.length());

		}

		gson = getGson();
	}

	public static Gson getGson() {
		OtherTypeAdapter otherTypeAdapter = new OtherTypeAdapter();
		return new GsonBuilder()
				.registerTypeHierarchyAdapter(Answer.class,
						new AnswerTypeAdapter())
				.registerTypeHierarchyAdapter(HostStatsEntry.class,
						otherTypeAdapter)
				.registerTypeAdapter(VirtualMachine.State.class,
						otherTypeAdapter)
				.registerTypeAdapter(HypervisorHostResourceSummary.class,
						otherTypeAdapter).create();
	}

	private HttpURLConnection openConnection(String command) throws IOException {
		URL url = new URL(host + command);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");

		return conn;
	}

	private void sendDataToServer(HttpURLConnection conn, String data) throws IOException {
		logger.info("Sending Data to Server: " + conn.toString());
		OutputStream os = conn.getOutputStream();
		os.write(data.getBytes());
		os.flush();
		os.close();

		if (conn.getResponseCode() == HttpURLConnection.HTTP_CREATED
				|| conn.getResponseCode() == HttpURLConnection.HTTP_OK)
			return;

		throw new IOException("Failed : HTTP error code : "
				+ conn.getResponseCode());
	}

	private String getServerResponse(HttpURLConnection conn) throws IOException {
		logger.info("Getting Server response: " + conn.toString());
		StringBuilder buff = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

		String output;
		while ((output = br.readLine()) != null) {
			buff.append(output);
		}

		return buff.toString();
	}

	public String send(String command, String json) throws IOException {
		logger.info("Sending to: " + command + " json: " + json);
		HttpURLConnection conn = openConnection(command.toString());

		//sendDataToServer(conn, json);
		String response = getServerResponse(conn);

		conn.disconnect();
		return stripJunk(response);
	}

	public String sendObject(String command, Object o) throws IOException {
		SortedMap<String, Object> map = new TreeMap<String, Object>();

		for (Field f : o.getClass().getFields()) {
			try {
				map.put(f.getName(), f.get(o));
			} catch (Exception e) {
				logger.debug("Field " + f.getName() + " problematic.");
			}

		}

		return sendData(command, map);
	}

	public Object sendData(String command, Map data, Class clazz) throws IOException {
		String jsonResponse;

		jsonResponse = sendData(command, data);

		logger.info("Response: " + jsonResponse);
		Object a = gson.fromJson(jsonResponse, clazz);

		return a;
	}

	public Object sendSimple(TitanAPIEnum command, Class clazz,
			Object... primitives) throws IOException {
		HashMap data = TitanAPIEnum.mapArgumentsToAPI(command, primitives);
		return sendData(command.toString(), data, clazz);
	}

	private static String stripJunk(String json) {
		int index1 = json.indexOf("{");
		int index2 = json.indexOf("[");

		if (index1 >= 0 && index2 >= 0 && index1 > index2)
			index1 = index2;

		return (index1 > 0) ? json.substring(index1, json.length()) : json;
	}

	public abstract String sendData(String command, Map<String, Object> map)
			throws IOException;

	public Object send(TitanAPIEnum command, Class clazz) throws IOException {
		return sendData(command.toString(), null, clazz);
	}

	public String send(TitanAPIEnum command, String json) throws IOException {
		return send(command.toString(), json);
	}

	public Object sendData(TitanAPIEnum command, Map data, Class clazz)
			throws IOException {
		return sendData(command.toString(), data, clazz);
	}

	public String sendData(TitanAPIEnum command, Map<String, Object> map)
			throws IOException {
		return sendData(command.toString(), map);
	}

	public Object sendObject(TitanAPIEnum command, Object o) throws IOException {
		return sendObject(command.toString(), o);
	}

	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		Serializer communicator = new UrlSerializer("192.168.137.1");

		Answer a = (Answer) communicator.sendSimple(TitanAPIEnum.PingTest,
				Answer.class);

		System.out.println(a.getDetails());
	}

}
