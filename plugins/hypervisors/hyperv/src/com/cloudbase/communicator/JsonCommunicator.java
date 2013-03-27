package com.cloudbase.communicator;

import java.io.IOException;
import java.util.Map;

public class JsonCommunicator extends Communicator {
	
	private static final String ARG = "arg";

	public JsonCommunicator(String host) {
		super(host);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String sendData(String command, Map<String, Object> map)
			throws IOException 
	{
		String json = gson.toJson(map);
		return this.send("?APICommand="+ command, json);
	}

	/*
	@Override
	public Object sendSimple(String command, Class clazz, Object... primitives) throws IOException 		
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		for(int i=0; i < primitives.length; i++)
			map.put(ARG+i, primitives[i]);
		
		return sendData(command, map, clazz);
	}
	
	*/

}
