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
