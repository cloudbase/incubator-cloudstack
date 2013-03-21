package com.cloudbase.gson;

import java.lang.reflect.Type;

import com.cloud.serializer.GsonHelper;
import com.cloud.vm.VirtualMachine;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class VmStateAdapter<T> implements JsonDeserializer<T> {
	private static final String OBJ = "obj";
	private static final Gson defaultJson;

	static {
		defaultJson = GsonHelper.getGson();
	}

	@Override
	public T deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {

		if (json == null || json.getAsString() == null)
			return null;

		String string = json.getAsString();

		T state = (T) VirtualMachine.State.valueOf(string);
		return state;

	}

}
