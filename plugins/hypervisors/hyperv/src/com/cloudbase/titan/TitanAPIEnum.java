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

	private TitanAPIEnum(String... arguments)
	{
		putInMap(this, arguments);
	}
	
	private TitanAPIEnum(boolean async, String... arguments)
	{
		this.async = async;
		putInMap(this, arguments);
	}

	private void putInMap(TitanAPIEnum command, String[] arguments)
	{
		if(argumentMaps == null)
			argumentMaps = new HashMap<TitanAPIEnum, String[]>();
		argumentMaps.put(command, arguments);
		
		if(commandMaps == null)
			commandMaps = new HashMap<String, TitanAPIEnum>();
		
		commandMaps.put(command.toString(), command);
	}

	public static HashMap<String, String> mapArgumentsToAPI(TitanAPIEnum command, Object... arguments)
	{
		HashMap<String, String> map = new HashMap<String, String>();
		String[] argumentNames = argumentMaps.get(command);

		for(int i=0; i<arguments.length && i<argumentNames.length; i++)
			map.put(argumentNames[i], arguments[i].toString());

		return map;
	}
	
	public static TitanAPIEnum getTitanCommand(String cmd)
	{
		return commandMaps.get(cmd);
	}
	
	public HashMap<String, String> mapArgumentsForCommand(Object[] args)
	{
		if(args == null || args.length == 0)
			return null;
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		String[] argsNames = getArgumentsForCommand(this);
		
		for(int i=0; i < args.length; i++)
			map.put(argsNames[i], args[i].toString());
		
		return map;
	}
	
	public static String[] getArgumentsForCommand(TitanAPIEnum cmd)
	{
		return argumentMaps.get(cmd);
	}
	
	public boolean isAsync()
	{
		return async;
	}
}
