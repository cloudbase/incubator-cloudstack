package com.cloudbase.titan;

import java.util.HashMap;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.hypervisor.mo.HypervisorHostResourceSummary;
import com.cloud.vm.VirtualMachine;
import com.cloudbase.AsyncAnswer;

public interface TitanAPI {
	Answer StartVMAsync(String vmName);

	Answer StopVMAsync(String vmName);

	Answer PauseVMAsync(String vmName);

	Answer RestartVMAsync(String vmName);

	Answer CreateVMAsync( String vmName, int cpuCount, Integer cpuSpeed, long memorySize);

	Answer SaveVMAsync(String vmName);

	Answer CloneVMAsync(String vmName);

	Answer DestroyVMAsync(String vmName);

	HashMap<String, String> GetVmStats(String vmNames);

	VirtualMachine.State CheckVirtualMachine(String vmName);

	/******************************************************************/
    /******************     Network Specific API     ******************/
    /******************************************************************/

    Answer AtachNetworkInterface(String vmName, String macAddress, String networkName, int mbps);
    
    Answer StripNetworkInterfaces(String vmName);

    Answer DetachNetworkInterface(String vmName, String macAddress);

    /******************************************************************/
    /******************     Storage Specific API     ******************/
    /******************************************************************/

    Answer MountStoragePool(String host, int port, String path, String username, String password);

    Answer UnmountStoragePool(String host, int port, String path);

	GetStorageStatsAnswer GetStorageStats();

	Answer CopyVolumeToSecondaryStorage(String secondaryStorageUrl, String volumePath);

	Answer CopyTemplateFromStorage(String storageUrl);
	
	String DownloadTemplateHttp(String url);
	
	Answer CreateVhdAsync(String templateId, long size, String vhdName);

	Answer AtachVhdAsync(String vhdName, String vmName);

	/******************************************************************/
	/******************     Snapshot Specific API     *****************/
	/******************************************************************/

	Answer CreateSnapshotAsync(String vmName);

	Answer DeleteSnapshot(String snapshotId, String snapshotPath);
	
	/******************************************************************/
	/*****************     Migration Specific API     *****************/
	/******************************************************************/
	
	Answer MigrateVmAsync(String vmName, String newHost);

	/******************************************************************/
	/********************     Host Specific API     *******************/
	/******************************************************************/

	Answer CheckOnHost();

	HostStatsEntry GetHostStats();

	HypervisorHostResourceSummary GetHostHardwareInfo();

	/******************************************************************/
	/************************     Other API     ***********************/
	/******************************************************************/

	Answer PingTest();

	Answer CheckHealth();

	Boolean Login(String username, String password);

	AsyncAnswer CheckAsyncJob(String job);

}
