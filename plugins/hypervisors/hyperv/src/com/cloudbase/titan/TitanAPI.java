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

	Answer CreateVMAsync(String vmName, int cpuCount, Integer cpuSpeed, long memorySize);

	Answer SaveVMAsync(String vmName);

	Answer CloneVMAsync(String vmName);

	Answer DestroyVMAsync(String vmName);

	HashMap<String, String> GetVmStats(String vmNames);

	VirtualMachine.State CheckVirtualMachine(String vmName);

	/******************************************************************/
	/****************** Network Specific API ******************/
	/******************************************************************/

	Answer AtachNetworkInterface(String vmName, String macAddress,
			String networkName, int mbps);

	Answer StripNetworkInterfaces(String vmName);

	Answer DetachNetworkInterface(String vmName, String macAddress);

	/******************************************************************/
	/****************** Storage Specific API ******************/
	/******************************************************************/

	Answer MountStoragePool(String host, int port, String path,
			String username, String password);

	Answer UnmountStoragePool(String host, int port, String path);

	GetStorageStatsAnswer GetStorageStats();

	Answer CopyVolumeToSecondaryStorage(String secondaryStorageUrl, String volumePath);

	Answer CopyTemplateFromStorage(String storageUrl);

	String DownloadTemplateHttp(String url);

	Answer CreateVhdAsync(String templateId, long size, String vhdName);
	
	Answer DestroyVhd(String vhdName);

	Answer AtachVhdAsync(String vhdName, String vmName);

	/******************************************************************/
	/****************** Snapshot Specific API *****************/
	/******************************************************************/

	Answer CreateSnapshotAsync(String vmName);

	Answer DeleteSnapshot(String snapshotId, String snapshotPath);

	/******************************************************************/
	/***************** Migration Specific API *****************/
	/******************************************************************/

	Answer MigrateVmAsync(String vmName, String newHost);

	/******************************************************************/
	/******************** Host Specific API *******************/
	/******************************************************************/

	Answer CheckOnHost();

	HostStatsEntry GetHostStats();

	HypervisorHostResourceSummary GetHostHardwareInfo();

	/******************************************************************/
	/************************ Other API ***********************/
	/******************************************************************/

	Answer PingTest();

	Answer CheckHealth();

	Boolean Login(String username, String password);

	AsyncAnswer CheckAsyncJob(String job);

}
