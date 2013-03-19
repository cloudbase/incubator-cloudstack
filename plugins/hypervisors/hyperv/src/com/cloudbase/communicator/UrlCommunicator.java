package com.cloudbase.communicator;

import java.io.IOException;
import java.util.Map;

public class UrlCommunicator extends Communicator 
{

	public UrlCommunicator(String host) {
		super(host);
	}

	public String sendData(String command, Map<String, Object> map) throws IOException
	{
		String query = mapToURLQuery(map);
		
		if(query != null && !query.equals(""))
			return this.send("?APICommand="+ command + "&" + query, " ");
		
		return this.send("?APICommand="+ command, " ");
	}
	
	/*
	
	public Object sendSimple(String command, Class clazz, Object... primitives) throws IOException
	{
		StringBuilder buff = new StringBuilder(command);
		
		buff.append("&");
		for(int i =0; i<primitives.length; i++)
			buff.append("arg").append(i).append("=").append(primitives[i]).append("&");
		
		buff.deleteCharAt(buff.length()-1);
		
		return send(buff.toString(), clazz);
	}
	
	*/
	
	private static String mapToURLQuery(Map<String, Object> map)
	{
		if(map == null || map.isEmpty())
			return "";
		
		StringBuilder buff = new StringBuilder();//.append("?");
		
		for(String key : map.keySet())
			buff.append(key).append("=").append(map.get(key)).append("&");
		
		buff.deleteCharAt(buff.length() - 1);
		
		return buff.toString();
	}

}
