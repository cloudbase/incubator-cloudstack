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
package com.cloudbase.gson;

import java.lang.reflect.Type;

import com.cloud.agent.api.Answer;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public class AnswerTypeAdapter implements JsonDeserializer<Answer> {
	private static final String OBJ = "obj";
	private static final String WAIT = "wait";
	private static final String MESSAGE = "details";
	private static final String RESULT = "result";

	private static final Gson defaultJson;

	static {
		defaultJson = GsonHelper.getGson();
	}

	@Override
	public Answer deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		Answer a = null;

		JsonObject jsonObject = json.getAsJsonObject();
		JsonElement prim = jsonObject.get(OBJ);
		if (prim != null && !prim.isJsonNull() && !prim.isJsonPrimitive())
			a = defaultJson.fromJson(prim, typeOfT);
		else
			return defaultJson.fromJson(json, typeOfT);

		try {
			prim = jsonObject.get(WAIT);
			if (prim != null && !prim.isJsonNull())
				a.setWait(((JsonPrimitive) prim).getAsInt());
		} catch (Exception e) {
			a.setWait(0);
		}

		try {
			prim = jsonObject.get(MESSAGE);
			if (prim != null && !prim.isJsonNull())
				a.setDetails(((JsonPrimitive) prim).getAsString());
		} catch (Exception e) {
			a.setDetails("");
		}

		try {
			prim = jsonObject.get(RESULT);
			if (prim != null && !prim.isJsonNull())
				a.setResult(((JsonPrimitive) prim).getAsBoolean());
		} catch (Exception e) {
			a.setDetails("");
		}

		return a;
	}

}
