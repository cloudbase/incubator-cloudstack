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

import java.io.IOException;
import java.util.Map;

public class UrlSerializer extends Serializer {

	public UrlSerializer(String host) {
		super(host);
	}

	public String sendData(String command, Map<String, Object> map)
			throws IOException {
		String query = mapToURLQuery(map);

		if (query != null && !query.equals(""))
			return this.send(command + "?" + query, " ");

		return this.send(command, " ");
	}

	private static String mapToURLQuery(Map<String, Object> map) {
		if (map == null || map.isEmpty())
			return "";

		StringBuilder buff = new StringBuilder();

		for (String key : map.keySet())
			buff.append(key).append("=").append(map.get(key)).append("&");

		buff.deleteCharAt(buff.length() - 1);

		return buff.toString();
	}

}
