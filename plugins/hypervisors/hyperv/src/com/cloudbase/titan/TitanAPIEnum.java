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

import java.util.HashMap;

public enum TitanAPIEnum {

	/*******************************************************************/
	/*********************     VM Specific API     *********************/
	/*******************************************************************/

	StartVMAsync(true, "vmName"),

	StopVMAsync(true, "vmName"),
	
	PauseVMAsync(true, "vmName"),

	RestartVMAsync(true, "vmName"),
	
	CreateVMAsync(true, "vmName", "cpuCount", "cpuSpeed", "memorySize"),

	SaveVMAsync(true, "vmName"),

	CloneVMAsync(true, "vmName"),

	DestroyVMAsync(true, "vmName"),

	GetVmStats("vmNames"),

	CheckVirtualMachine("vmName"),
	
	/******************************************************************/
    /******************     Network Specific API     ******************/
    /******************************************************************/

    AtachNetworkInterface("vmName", "macAddress", "networkName", "mbps"),
    
    StripNetworkInterfaces("vmName"),
    
    DetachNetworkInterface("vmName", "macAddress"),

	/******************************************************************/
	/******************     Storage Specific API     ******************/
	/******************************************************************/

	MountStoragePool("host", "port", "path", "username", "password"),
	
	UnmountStoragePool("host", "port"),

	GetStorageStats(),

	CopyVolumeToSecondaryStorage("secondaryStorageUrl", "volumePath"),

	CopyTemplateFromStorage(true, "storageUrl"),
	
	DownloadTemplateHttp(true, "url"),
	
	CreateVhdAsync(true, "templateId", "size", "vhdName"),
	
	DestroyVhd("vhdName"),

	AtachVhdAsync(true, "vhdName", "vmName"),

	/******************************************************************/
	/******************     Snapshot Specific API     *****************/
	/******************************************************************/

	CreateSnapshotAsync(true, "vmName"),

	DeleteSnapshot("snapshotId", "snapshotPath"),
	
	/******************************************************************/
	/*****************     Migration Specific API     *****************/
	/******************************************************************/
	
	MigrateVmAsync("vmName", "newHost"),

	/******************************************************************/
	/********************     Host Specific API     *******************/
	/******************************************************************/

	CheckOnHost(),

	GetHostStats(),
	
	GetHostHardwareInfo(),

	/******************************************************************/
	/************************     Other API     ***********************/
	/******************************************************************/

	PingTest(),

	CheckHealth(),

	Login("username", "password"),

	CheckAsyncJob("job");


	private static HashMap<TitanAPIEnum, String[]> argumentMaps;
	private static HashMap<String, TitanAPIEnum> commandMaps;
	private boolean async = false;

	private TitanAPIEnum(String... arguments) {
		putInMap(this, arguments);
	}

	private TitanAPIEnum(boolean async, String... arguments) {
		this.async = async;
		putInMap(this, arguments);
	}

	private void putInMap(TitanAPIEnum command, String[] arguments) {
		if (argumentMaps == null)
			argumentMaps = new HashMap<TitanAPIEnum, String[]>();
		argumentMaps.put(command, arguments);

		if (commandMaps == null)
			commandMaps = new HashMap<String, TitanAPIEnum>();

		commandMaps.put(command.toString(), command);
	}

	public static HashMap<String, String> mapArgumentsToAPI(
			TitanAPIEnum command, Object... arguments) {
		HashMap<String, String> map = new HashMap<String, String>();
		String[] argumentNames = argumentMaps.get(command);

		for (int i = 0; i < arguments.length && i < argumentNames.length; i++)
			map.put(argumentNames[i], arguments[i].toString());

		return map;
	}

	public static TitanAPIEnum getTitanCommand(String cmd) {
		return commandMaps.get(cmd);
	}

	public HashMap<String, String> mapArgumentsForCommand(Object[] args) {
		if (args == null || args.length == 0)
			return null;

		HashMap<String, String> map = new HashMap<String, String>();

		String[] argsNames = getArgumentsForCommand(this);

		for (int i = 0; i < args.length; i++)
			map.put(argsNames[i], args[i].toString());

		return map;
	}

	public static String[] getArgumentsForCommand(TitanAPIEnum cmd) {
		return argumentMaps.get(cmd);
	}

	public boolean isAsync() {
		return async;
	}
}
