/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.framework.messaging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RpcServiceDispatcher {

	private static Map<Class<?>, Method> s_handlerCache = new HashMap<Class<?>, Method>();
	
	public static boolean dispatch(Object target, RpcServerCall serviceCall) {
		assert(serviceCall != null);
		assert(target != null);
		
		Method handler = resolveHandler(target.getClass(), serviceCall.getCommand());
		if(handler == null)
			return false;
		
		try {
			handler.invoke(target, serviceCall);
		} catch (IllegalArgumentException e) {
			throw new RpcException("IllegalArgumentException when invoking RPC service command: " + serviceCall.getCommand());
		} catch (IllegalAccessException e) {
			throw new RpcException("IllegalAccessException when invoking RPC service command: " + serviceCall.getCommand());
		} catch (InvocationTargetException e) {
			throw new RpcException("InvocationTargetException when invoking RPC service command: " + serviceCall.getCommand());
		}
		
		return true;
	}
	
	public static Method resolveHandler(Class<?> handlerClz, String command) {
		synchronized(s_handlerCache) {
			Method handler = s_handlerCache.get(handlerClz);
			if(handler != null)
				return handler;
			
			for(Method method : handlerClz.getMethods()) {
				RpcServiceHandler annotation = method.getAnnotation(RpcServiceHandler.class);
				if(annotation != null) {
					if(annotation.command().equals(command)) {
						s_handlerCache.put(handlerClz, method);
						return method;
					}
				}
			}
		}
		
		return null;
	}
}