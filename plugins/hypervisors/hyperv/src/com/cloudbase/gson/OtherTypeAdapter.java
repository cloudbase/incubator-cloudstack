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

import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class OtherTypeAdapter<T> implements JsonDeserializer<T> {

	private static final String OBJ = "obj";

	private static final Gson defaultJson;

	static {
		defaultJson = GsonHelper.getGson();
	}

	@Override
	public T deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		T a = null;

		JsonObject jsonObject = json.getAsJsonObject();
		JsonElement prim = jsonObject.get(OBJ);
		if (prim != null)
			a = defaultJson.fromJson(prim, typeOfT);

		return a;
	}

}
