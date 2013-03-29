// Copyright 2013 Cloudbase Solutions Srl
// Copyright 2012 Citrix Systems, Inc. 
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
package com.cloud.hypervisor.hyperv.resource;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PoolEjectCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.ValidateSnapshotCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.mo.HypervisorHostResourceSummary;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.ExternalUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloudbase.titan.TitanAPI;
import com.cloudbase.titan.TitanAPIException;
import com.cloudbase.titan.TitanAPIHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Implementation of resource base class for HyperV hypervisor
 **/

@SuppressWarnings({ "unchecked", "unused" })
@Local(value = { ServerResource.class })
public class HypervResource extends ServerResourceBase implements ServerResource {
	
	private static final Logger s_logger = Logger.getLogger(HypervResource.class);
	private static final long MEGA = 1024 * 1024;
	private static final long GIGA = 1024 * 1024 * 1024;
	private static final long TERA = 1024 * 1024 * 1024 * 1024;
	private static final long _ops_timeout = 150000; // 2.5 minutes
	private static final int _retry = 24;
	private static final String ROUTER_IP = "router.ip";
	private static final String COMMAND = "Command";

	protected Gson _gson;
	protected HashMap<String, State> _vms = new HashMap<String, State>(512);
	protected final int DEFAULT_DOMR_SSHPORT = 3922;
	protected final int DEFAULT_SSHPORT = 22;
	
	private String _dcId;
	private String _podId;
	private String _clusterId;
	private String _guid;
	private String _name;
	private String _version;
	
	private HashMap<String, String> _ipMap = new HashMap<String, String>();

	private String _hypervHostUrl;
	private String _username;
	private String _password;

	private TitanAPI remote;

	private IAgentControl agentControl;
	private volatile Boolean _wakeUp = false;

	public HypervResource() {
		_gson = new GsonBuilder().create();
	}

	@Override
	public Answer executeRequest(Command cmd) {
		s_logger.info("Received command: " + cmd.getClass().getSimpleName() + " "
				+ _gson.toJson(cmd));

		Class<Answer> answerClass;
		Method m;

		try {
			// check for a specific command execution.
			m = this.getClass().getDeclaredMethod("execute",
					new Class[] { cmd.getClass() });
			if (m != null)
				return (Answer) m.invoke(this, cmd);

			// no specific command implementation found. Execute generic
			// execution.

			answerClass = (Class<Answer>) Class.forName(cmd.getClass()
					.getCanonicalName().replace("Command", "Answer"));

			if (answerClass == null)
				answerClass = Answer.class;

			// return execute(cmd, answerClass);

			return Answer.createUnsupportedCommandAnswer(cmd);

		} catch (Exception e) {
			s_logger.error(e.getMessage());
			e.printStackTrace();
			return new Answer(cmd, e);
		}

	}

	// TODO: start

	/*******************************************************************/
	/************************* VM Specific API *************************/
	/*******************************************************************/

	protected StartAnswer execute(StartCommand cmd) throws TitanAPIException,
			Exception {

		VirtualMachineTO vm = cmd.getVirtualMachine();
		String vmName = vm.getName();

		State vmState = remote.CheckVirtualMachine(vmName);
		if (vmState != null || !State.isVmDestroyed(vmState, null, vmState))
			remote.StripNetworkInterfaces(vmName);

		remote.CreateVMAsync(vmName, vm.getCpus(), vm.getMaxSpeed(),
				vm.getMaxRam());

		for (VolumeTO disk : vm.getDisks())
			remote.AtachVhdAsync(disk.getName(), vmName);

		for (NicTO nic : vm.getNics())
			if (nic.isDefaultNic()) {
				_ipMap.put(vmName, nic.getIp());
				break;
			}

		for (NicTO nic : vm.getNics())
		{
			remote.AtachNetworkInterface(vmName, nic.getMac(),
					nic.getBroadcastType().toString(), nic.getNetworkRateMbps());
			
			ExternalUtils.registerLocalDhcpEntry(nic.getMac(), nic.getIp(), vmName + "-"
					+ nic.getType().toString());
		}

		remote.StartVMAsync(vmName);

		return new StartAnswer(cmd);
	}

	private StopAnswer execute(StopCommand cmd) throws TitanAPIException {

		remote.StopVMAsync(cmd.getVmName());

		StopAnswer answer = new StopAnswer(cmd, "Dummy "
				+ Answer.class.getSimpleName() + " from Hyperv", true);

		return answer;
	}

	/******************************************************************/
	/*********************** Storage Specific API *********************/
	/******************************************************************/

	public Answer execute(CreateCommand cmd) throws TitanAPIException {
		DiskProfile diskChar = cmd.getDiskCharacteristics();
		StorageFilerTO pool = cmd.getPool();

		remote.CreateVhdAsync(cmd.getTemplateUrl(), diskChar.getSize(),
				diskChar.getName());

		VolumeTO vol = new VolumeTO(cmd.getVolumeId(), diskChar.getType(),
				pool.getType(), pool.getUuid(), diskChar.getName(),
				pool.getHost(), pool.getPath(), diskChar.getSize(), null);

		return new CreateAnswer(cmd, vol);
	}

	private Answer execute(CreateStoragePoolCommand cmd)
			throws TitanAPIException {
		StorageFilerTO pool = cmd.getPool();
		String path = pool.getHost() + ":" + pool.getPort() + pool.getPath();
		return remote.MountStoragePool(pool.getHost(), pool.getPort(),
				pool.getPath(), "user", "password");
	}

	protected Answer execute(ModifyStoragePoolCommand cmd) {
		StorageFilerTO pool = cmd.getPool();
		Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
		// FIXME: get the actual storage capacity and storage stats of CSV
		// volume
		// by running powershell cmdlet. This hardcoding just for prototype.
		ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd,

		1024 * 1024 * 1024 * 1024L, 512 * 1024 * 1024 * 1024L, tInfo);

		return answer;

	}

	protected Answer execute(GetStorageStatsCommand cmd) throws TitanAPIException {
		return remote.GetStorageStats();
	}

	public PrimaryStorageDownloadAnswer execute(PrimaryStorageDownloadCommand cmd) throws TitanAPIException {
		
		String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
		String primaryStroageUrl = cmd.getPrimaryStorageUrl();
		String templateUuidName = null;
		assert ((primaryStroageUrl != null) && (secondaryStorageUrl != null));
		String templateUrl = cmd.getUrl();//.replace("//", "/");
		if (!templateUrl.endsWith(".vhd"))
			templateUrl = templateUrl
					+ UUID.nameUUIDFromBytes(cmd.getName().getBytes()) + ".vhd";

		templateUuidName = remote.DownloadTemplateHttp(templateUrl);
		s_logger.info("template URL: " + templateUrl + "template name: "
				+ cmd.getName() + "  sec storage " + secondaryStorageUrl
				+ " pri storage" + primaryStroageUrl);
		return new PrimaryStorageDownloadAnswer(templateUuidName, 0);

	}
	
	private Answer execute(DestroyCommand cmd) throws TitanAPIException {
		return remote.DestroyVhd(cmd.getVolume().getName());
	}

	/******************************************************************/
	/************************* Host Specific API **********************/
	/******************************************************************/

	private Answer execute(GetHostStatsCommand cmd) throws TitanAPIException {
		HostStatsEntry stats = remote.GetHostStats();
		return new GetHostStatsAnswer(cmd, stats);
	}

	private RebootAnswer execute(RebootCommand cmd) throws TitanAPIException {
		remote.RestartVMAsync(cmd.getVmName());
		RebootAnswer answer = new RebootAnswer(cmd, "Dummy "
				+ RebootAnswer.class.getSimpleName() + " from Hyperv", 9999);
		return answer;
	}

	private Answer execute(ReadyCommand cmd) throws TitanAPIException {
		return new ReadyAnswer(cmd);
	}

	private Answer execute(GetDomRVersionCmd cmd) throws TitanAPIException {
		return new GetDomRVersionAnswer(cmd, "Dummy DomRVersion response", "",
				"");
	}

	protected Answer execute(DhcpEntryCommand cmd) throws Exception {
		ExternalUtils.registerDhcpEntry(cmd.getVmMac(), cmd.getVmIpAddress(),
				cmd.getVmName(), cmd.getAccessDetail(ROUTER_IP),
				DEFAULT_DOMR_SSHPORT);
		return new Answer(cmd);
	}

	protected CheckSshAnswer execute(CheckSshCommand cmd) throws Exception {
		String vmName = cmd.getName();
		String privateIp = cmd.getIp();
		int cmdPort = cmd.getPort();

		s_logger.info("Ping VM:" + cmd.getName() + " IP:" + privateIp + " port:"
				+ cmdPort);
		String result = ExternalUtils.connect(privateIp, cmdPort);
		if (result != null) {
			result = ExternalUtils.connect(privateIp);

			if (result != null) {
				String details = "Cannot ping System vm " + vmName
						+ " due to result: " + result;
				s_logger.error(details);
				return new CheckSshAnswer(cmd, details);
			}
		}

		return new CheckSshAnswer(cmd);
	}

	// TODO
	private Answer execute(SavePasswordCommand cmd) {
		return new Answer(cmd);
	}

	private Answer execute(AttachVolumeCommand cmd) {
		return new AttachVolumeAnswer(cmd);
	}

	private Answer execute(CopyVolumeCommand cmd) {
		return new CopyVolumeAnswer(cmd, true, "", cmd.getVolumePath(),
				cmd.getVolumePath());
	}

	private Answer execute(DeleteStoragePoolCommand cmd) {
		return new Answer(cmd);
	}

	private MigrateAnswer execute(MigrateCommand cmd) {
		MigrateAnswer answer = new MigrateAnswer(cmd, true, "Dummy "
				+ MigrateAnswer.class.getSimpleName() + " from Hyperv", 9999);
		return answer;
	}

	private CheckVirtualMachineAnswer execute(CheckVirtualMachineCommand cmd) {
		CheckVirtualMachineAnswer answer = new CheckVirtualMachineAnswer(cmd,
				State.Running, 9999, "Dummy "
						+ CheckVirtualMachineAnswer.class.getSimpleName()
						+ " from Hyperv");
		return answer;
	}

	private Answer execute(RebootRouterCommand cmd) {
		Answer answer = new Answer(cmd, true, "Dummy "
				+ Answer.class.getSimpleName() + " from Hyperv");
		return answer;
	}

	// TODO
	private Answer execute(CheckHealthCommand cmd) {
		return new Answer(cmd);
	}

	private Answer execute(GetVmStatsCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(VmDataCommand cmd) {
		return new Answer(cmd);
	}

	private Answer execute(LoadBalancerConfigCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(SetStaticNatRulesCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(SetPortForwardingRulesCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(ManageSnapshotCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(BackupSnapshotCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(CreateVolumeFromSnapshotCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(CreatePrivateTemplateFromVolumeCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(CreatePrivateTemplateFromSnapshotCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	// TODO
	private Answer execute(GetVncPortCommand cmd) {
		
		String address = _ipMap.get(cmd.getName());
		
		if( address == null)
		{
			s_logger.warn("VNC Entry not found for: " + cmd.getName());
			return new GetVncPortAnswer(cmd, "VNC Entry not found for: " + cmd.getName());
		}
		
		return new GetVncPortAnswer(cmd, address, 5901);
		// return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(AttachIsoCommand cmd) {
		return new Answer(cmd);
	}

	private Answer execute(ValidateSnapshotCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private SetupAnswer execute(SetupCommand cmd) {
		// TODO Auto-generated method stub
		SetupAnswer answer = new SetupAnswer(cmd, "Dummy "
				+ SetupAnswer.class.getSimpleName() + " from Hyperv");
		return answer;
	}

	private Answer execute(ModifySshKeysCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(PoolEjectCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
		// TODO Auto-generated method stub
		CheckOnHostAnswer answer = new CheckOnHostAnswer(cmd, true, "Dummy "
				+ CheckOnHostAnswer.class.getSimpleName() + " from Hyperv");

		return answer;
	}

	private Answer execute(PingTestCommand cmd) {
		// TODO Auto-generated method stub
		Answer answer = new Answer(cmd, true, "Dummy "
				+ Answer.class.getSimpleName() + " from Hyperv");
		return answer;
	}

	private NetworkUsageAnswer execute(NetworkUsageCommand cmd) {
		// TODO Auto-generated method stub
		NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "Dummy "
				+ NetworkUsageAnswer.class.getSimpleName() + " from Hyperv",
				(long) 1024 * 1024 * 1024, (long) 1024 * 1024 * 1024 * 100);
		return answer;
	}

	private Answer execute(CleanupNetworkRulesCmd cmd) {

		return new Answer(cmd, true, "Dummy " + cmd.getClass().getSimpleName()
				+ " from Hyperv");
	}
	
	private Answer execute(SecurityGroupRulesCmd cmd) {
		return  new Answer(cmd, true, "Dummy " + cmd.getClass().getSimpleName()
				+ " from Hyperv");
	}

	private Answer execute(RemoteAccessVpnCfgCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	private Answer execute(VpnUsersCfgCommand cmd) {
		return Answer.createUnsupportedCommandAnswer(cmd);
	}

	// TODO
	private MaintainAnswer execute(MaintainCommand cmd) {

		MaintainAnswer answer = new MaintainAnswer(cmd, true, "Dummy "
				+ CheckNetworkAnswer.class.getSimpleName() + " from Hyperv");
		return answer;
	}

	private CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
		CheckNetworkAnswer answer = new CheckNetworkAnswer(cmd, true, "Dummy "
				+ CheckNetworkAnswer.class.getSimpleName() + " from Hyperv");
		return answer;
	}

	/******************************************************************/
	/************************** Miscellaneous *************************/
	/******************************************************************/

	protected HashMap<String, State> sync() {
		HashMap<String, State> changes = new HashMap<String, State>();

		try {
			synchronized (_vms) {
			}

		} catch (Throwable e) {
			s_logger.error(
					"Unable to perform sync information collection process at this point due to exception ",
					e);
			return null;
		}
		return changes;
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		HashMap<String, State> newStates = sync();
		if (newStates == null) {
			newStates = new HashMap<String, State>();
		}
		PingRoutingCommand cmd = new PingRoutingCommand(
				com.cloud.host.Host.Type.Routing, id, newStates);
		return cmd;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		this.agentControl = agentControl;
	}

	@Override
	public Type getType() {
		return Type.Routing;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}

	/******************************************************************/
	/********************** Init & Config *********************/
	/******************************************************************/

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {

		_dcId = params.get("zone").toString();
		_podId = params.get("pod").toString();
		_clusterId = params.get("cluster").toString();
		_guid = params.get("guid").toString();
		_version = params.get("version").toString();
		_name = name;
		_hypervHostUrl = params.get("url").toString();
		_username = params.get("username").toString();
		_password = params.get("password").toString();

		boolean success = super.configure(name, params);
		if (!success) {
			return false;
		}

		try {
			remote = (TitanAPI) TitanAPIHandler
					.getTitanAPIInstance(_hypervHostUrl);
			return remote.Login(_username, _password);

		} catch (Exception e) {
			s_logger.error(e.getMessage());
			e.printStackTrace();
			throw new ConfigurationException();
		}
	}

	@Override
	public StartupCommand[] initialize() {

		// TODO: Work here.

		s_logger.info("recieved initialize request for cluster:" + _clusterId);
		List<String> vmHostList = getHostsInCluster(_clusterId);

		if (vmHostList.size() == 0) {
			s_logger.info("cluster is not recognized or zero instances in the cluster");
		}

		StartupCommand[] answerCmds = new StartupCommand[vmHostList.size()];

		int index = 0;
		for (String hostName : vmHostList) {
			s_logger.info("Node :" + hostName);

			StartupRoutingCommand cmd = new StartupRoutingCommand();
			fillHostInfo(cmd, hostName);

			answerCmds[index] = cmd;
			index++;
		}

		s_logger.info("response sent to initialize request for cluster:"
				+ _clusterId);
		return answerCmds;
	}

	protected void fillHostInfo(StartupRoutingCommand cmd, String hostName) {

		Map<String, String> details = cmd.getHostDetails();
		if (details == null) {
			details = new HashMap<String, String>();
		}

		try {
			fillHostHardwareInfo(cmd);
			fillHostNetworkInfo(cmd);
			fillHostDetailsInfo(details);
		} catch (Exception e) {
			s_logger.error("Exception while retrieving host info ", e);
			throw new CloudRuntimeException(
					"Exception while retrieving host info");
		}

		cmd.setName(hostName);
		cmd.setHostDetails(details);
		cmd.setGuid(_guid);
		cmd.setDataCenter(_dcId);
		cmd.setPod(_podId);
		cmd.setCluster(_clusterId);
		cmd.setHypervisorType(HypervisorType.Hyperv);
		cmd.setHypervisorVersion(_version);
	}

	private void fillHostDetailsInfo(Map<String, String> details)
			throws Exception {

	}

	private void fillHostHardwareInfo(StartupRoutingCommand cmd)
			throws RemoteException {
		try {
			HypervisorHostResourceSummary resourceSummary = remote
					.GetHostHardwareInfo();
			cmd.setCaps("hvm");
			cmd.setDom0MinMemory(0);
			cmd.setSpeed(resourceSummary.getCpuSpeed()); // 100.000GHz
			cmd.setCpus((int) resourceSummary.getCpuCount());
			cmd.setMemory(resourceSummary.getMemoryBytes());
		} catch (Throwable e) {
			s_logger.error("Unable to query host network info due to exception ",
					e);
			throw new CloudRuntimeException(
					"Unable to query host network info due to exception");
		}
	}

	private void fillHostNetworkInfo(StartupRoutingCommand cmd)
			throws RemoteException {
		try {
			// FIXME: get the actual host and storage IP by running cmdlet.This
			// hardcoding just for prototype
			cmd.setPrivateIpAddress(_hypervHostUrl);
			cmd.setPrivateNetmask("255.255.0.0");
			cmd.setPrivateMacAddress("00:16:3e:77:e2:a0");

			cmd.setStorageIpAddress("10.73.76.41");
			cmd.setStorageNetmask("255.255.0.0");
			cmd.setStorageMacAddress("00:15:5D:98:0C:0C");
		} catch (Throwable e) {
			s_logger.error("Unable to query host network info due to exception ",
					e);
			throw new CloudRuntimeException(
					"Unable to query host network info due to exception");
		}
	}

	private List<String> getHostsInCluster(String clusterName) {
		List<String> returned = new LinkedList<String>();
		returned.add(_hypervHostUrl);
		return returned;
	}
	
	private List<NicTO> sortNics(List<NicTO> nics) {
		List<NicTO> sorted = new LinkedList<NicTO>();
		Collections.copy(sorted, nics);
		
		Collections.sort(nics, new Comparator<NicTO>() {
			@Override
			public int compare(NicTO o1, NicTO o2) {
				return new Integer(o1.getDeviceId()).compareTo(o2.getDeviceId());
			}
		});
		
		return sorted;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub

	}
}
