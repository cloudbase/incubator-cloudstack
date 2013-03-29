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
package com.cloud.hypervisor.hyperv;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.transport.Request;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.hyperv.resource.HypervResource;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.Task;
import com.cloud.utils.nio.Task.Type;

@Local(value = Discoverer.class)
public class HypervServerDiscoverer extends DiscovererBase implements Discoverer, HandlerFactory, ResourceStateAdapter {
	
	private static final Logger s_logger = Logger.getLogger(HypervServerDiscoverer.class);
	
	@Inject
	ClusterDao _clusterDao;
	
	@Inject
	AlertManager _alertMgr;
	
	@Inject
	ClusterDetailsDao _clusterDetailsDao;
	
	@Inject
	HostDao _hostDao = null;
	
	@Inject
	ResourceManager _resourceMgr;
	
	Link _link;

	@Override
	public Map<? extends ServerResource, Map<String, String>> find(long dcId,
			Long podId, Long clusterId, URI url, String username,
			String password, List<String> hostTags) throws DiscoveryException {

		if (s_logger.isInfoEnabled()) {
			s_logger.info("Discover host. dc: " + dcId + ", pod: " + podId
					+ ", cluster: " + clusterId + ", uri host: "
					+ url.getHost());
		}

		if (podId == null) {
			if (s_logger.isInfoEnabled()) {
				s_logger.info("No pod is assigned, skipping the discovery in Hyperv discoverer");
			}
			return null;
		}

		if (!url.getScheme().equals("http")) {
			String msg = "urlString is not http so HypervServerDiscoverer taking care of the discovery for this: "
					+ url;
			s_logger.debug(msg);
			return null;
		}

		ClusterVO cluster = _clusterDao.findById(clusterId);
		if (cluster == null
				|| cluster.getHypervisorType() != HypervisorType.Hyperv) {
			if (s_logger.isInfoEnabled()) {
				s_logger.info("invalid cluster id or cluster is not for Hyperv hypervisors");
			}
			return null;
		}
		
		try {

			String hostname = url.getHost();
			InetAddress ia = InetAddress.getByName(hostname);
			String agentIp = ia.getHostAddress();
			String guid = UUID.nameUUIDFromBytes(agentIp.getBytes()).toString();
			String guidWithTail = guid + "-HypervResource";/*
															 * tail added by
															 * agent.java
															 */
			if (_resourceMgr.findHostByGuid(guidWithTail) != null) {
				s_logger.debug("Skipping " + agentIp + " because "
						+ guidWithTail + " is already in the database.");
				return null;
			}

			Map<HypervResource, Map<String, String>> resources = new HashMap<HypervResource, Map<String, String>>();
			Map<String, String> details = new HashMap<String, String>();
			Map<String, Object> params = new HashMap<String, Object>();
			HypervResource resource = new HypervResource();
			
			details.put("url", url.getHost());
			details.put("username", username);
			details.put("password", password);
			details.put("guid", guid);
			details.put("version", "2.0");
			resources.put(resource, details);

			params.put("url", hostname);
			params.put("username", username);
			params.put("password", password);
			params.put("guid", guid);
			params.put("zone", Long.toString(dcId));
			params.put("pod", Long.toString(podId));
			params.put("cluster", Long.toString(clusterId));
			params.put("version", "2.0");

			try {
				resource.configure("Hyperv", params);
			} catch (ConfigurationException e) {
				_alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId,
						"Unable to add " + url.getHost(),
						"Error is " + e.getMessage());
				s_logger.warn("Unable to instantiate " + url.getHost(), e);
			}

			resource.start();

			cluster.setGuid(UUID.nameUUIDFromBytes(
					String.valueOf(clusterId).getBytes()).toString());
			_clusterDao.update(clusterId, cluster);

			return resources;

		} catch (UnknownHostException e) {
			_alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId,
					"Unable to add " + url.getHost(),
					"Error is " + e.getMessage());
			s_logger.warn("Unable to instantiate " + url.getHost(), e);
			e.printStackTrace();
		} catch (Exception e) {
			s_logger.info("exception " + e.toString());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void postDiscovery(List<HostVO> hosts, long msId) {
		// do nothing
	}

	@Override
	public boolean matchHypervisor(String hypervisor) {
		if (hypervisor == null) {
			return true;
		}

		return Hypervisor.HypervisorType.Hyperv.toString().equalsIgnoreCase(
				hypervisor);
	}

	@Override
	public Hypervisor.HypervisorType getHypervisorType() {
		return Hypervisor.HypervisorType.Hyperv;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		if (s_logger.isInfoEnabled())
			s_logger.info("Configure HypervServerDiscoverer, discover name: "
					+ name);

		super.configure(name, params);

		// TODO: Component Locator? getDao?

		if (s_logger.isInfoEnabled()) {
			s_logger.info("HypervServerDiscoverer has been successfully configured");
		}
		_resourceMgr.registerResourceStateAdapter(this.getClass()
				.getSimpleName(), this);

		return true;
	}

	@Override
	public Task create(Type type, Link link, byte[] data) {
		_link = link;
		return new BootStrapTakHandler(type, link, data);
	}

	// class to handle the bootstrap command from the management server
	public class BootStrapTakHandler extends Task {

		public BootStrapTakHandler(Task.Type type, Link link, byte[] data) {
			super(type, link, data);
			s_logger.info("created new BootStrapTakHandler");
		}

		protected void processRequest(final Link link, final Request request) {

		}

		@Override
		protected void doTask(Task task) throws Exception {
			final Type type = task.getType();
			s_logger.info("recieved task of type " + type.toString()
					+ " in BootStrapTakHandler");
		}
	}

	@Override
	public HostVO createHostVOForConnectedAgent(HostVO host,
			StartupCommand[] cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostVO createHostVOForDirectConnectAgent(HostVO host,
			StartupCommand[] startup, ServerResource resource,
			Map<String, String> details, List<String> hostTags) {

		StartupCommand firstCmd = startup[0];
		if (!(firstCmd instanceof StartupRoutingCommand)) {
			return null;
		}

		StartupRoutingCommand ssCmd = ((StartupRoutingCommand) firstCmd);
		if (ssCmd.getHypervisorType() != HypervisorType.Hyperv) {
			return null;
		}

		return _resourceMgr.fillRoutingHostVO(host, ssCmd,
				HypervisorType.Hyperv, details, hostTags);
	}

	@Override
	public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
			boolean isForceDeleteStorage) throws UnableDeleteHostException {

		if (host.getType() != com.cloud.host.Host.Type.Routing
				|| host.getHypervisorType() != HypervisorType.Hyperv) {
			return null;
		}

		_resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
		return new DeleteHostAnswer(true);
	}

	@Override
	public boolean stop() {
		_resourceMgr.unregisterResourceStateAdapter(this.getClass()
				.getSimpleName());
		return super.stop();
	}
}
