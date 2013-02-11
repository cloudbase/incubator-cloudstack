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
package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConfigureNexusVsmForAsaCommand;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalFirewallCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.host.Host;
import com.cloud.network.cisco.CiscoVnmcConnectionImpl;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.OperationType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
import com.cloud.utils.exception.ExecutionException;

public class CiscoVnmcResource implements ServerResource{

    private String _name;
    private String _zoneId;
    private String _physicalNetworkId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private Integer _numRetries;
    private String _publicZone;
    private String _privateZone;
    private String _publicInterface;
    private String _privateInterface;

	CiscoVnmcConnectionImpl _connection;

    private final Logger s_logger = Logger.getLogger(CiscoVnmcResource.class);

    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand) cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand) cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand) cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand) cmd);
        } else if (cmd instanceof CreateLogicalEdgeFirewallCommand) {
            return execute((CreateLogicalEdgeFirewallCommand)cmd);
        } else if (cmd instanceof ConfigureNexusVsmForAsaCommand) {
        	return execute((ConfigureNexusVsmForAsaCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String) params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            _zoneId = (String) params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _physicalNetworkId = (String) params.get("physicalNetworkId");
            if (_physicalNetworkId == null) {
                throw new ConfigurationException("Unable to find physical network id in the configuration parameters");
            }

            _ip = (String) params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _username = (String) params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username");
            }

            _password = (String) params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password");
            }			

            _publicInterface = (String) params.get("publicinterface");
            if (_publicInterface == null) {
                //throw new ConfigurationException("Unable to find public interface.");
            }

            _privateInterface = (String) params.get("privateinterface");
            if (_privateInterface == null) {
                //throw new ConfigurationException("Unable to find private interface.");
            }

            _publicZone = (String) params.get("publiczone");
            if (_publicZone == null) {
                _publicZone = "untrust";
            }

            _privateZone = (String) params.get("privatezone");
            if (_privateZone == null) {
                _privateZone = "trust";
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String) params.get("numretries"), 1);

            NumbersUtil.parseInt((String) params.get("timeout"), 300);

            // Open a socket and login
            _connection = new CiscoVnmcConnectionImpl(_ip, _username, _password);
            if (!refreshVnmcConnection()) {
                throw new ConfigurationException("Unable to open a connection to the VNMC.");
            }

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

    }

    public StartupCommand[] initialize() {   
        StartupExternalFirewallCommand cmd = new StartupExternalFirewallCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion("");
        cmd.setGuid(_guid);
        return new StartupCommand[] { cmd };
    }

    public Host.Type getType() {
        return Host.Type.ExternalFirewall;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.ExternalFirewall, id);
    }

    @Override
    public void disconnected() {
    }

    public IAgentControl getAgentControl() {
        return null;
    }

    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private ExternalNetworkResourceUsageAnswer execute(ExternalNetworkResourceUsageCommand cmd) {
    	return new ExternalNetworkResourceUsageAnswer(cmd);
    }

    /*
     * Login
     */
    private boolean refreshVnmcConnection() {
        boolean ret = false;
        try {
            ret = _connection.login();
        } catch (ExecutionException ex) {
        	s_logger.error("Login to Vnmc failed", ex);
        }
        return ret;
    }

    private synchronized Answer execute(IpAssocCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(IpAssocCommand cmd, int numRetries) {        
        String[] results = new String[cmd.getIpAddresses().length];
        return new IpAssocAnswer(cmd, results);
    }

    /*
     * Static NAT
     */
    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        return new Answer(cmd);
    }

    /*
     * Destination NAT
     */
    private synchronized Answer execute(SetPortForwardingRulesCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {
        return new Answer(cmd);
    }

    /*
     * Logical edge firewall
     */
    private synchronized Answer execute(CreateLogicalEdgeFirewallCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(CreateLogicalEdgeFirewallCommand cmd, int numRetries) {
        String tenant = "vlan-" + cmd.getVlanId();
        try {
            // create tenant
            if (!_connection.createTenant(tenant))
            	throw new Exception("Failed to create tenant in VNMC for guest network with vlan " + cmd.getVlanId());

            // create tenant VDC
            if (!_connection.createTenantVDC(tenant))
            	throw new Exception("Failed to create tenant VDC in VNMC for guest network with vlan " + cmd.getVlanId());

            // create edge security profile
            if (!_connection.createTenantVDCEdgeSecurityProfile(tenant))
            	throw new Exception("Failed to create tenant edge security profile in VNMC for guest network with vlan " + cmd.getVlanId());

            // create logical edge firewall
            if (!_connection.createEdgeFirewall(tenant, cmd.getPublicIp(), cmd.getInternalIp(), cmd.getPublicSubnet(), cmd.getInternalSubnet()))
            	throw new Exception("Failed to create edge firewall in VNMC for guest network with vlan " + cmd.getVlanId());
        } catch (Throwable e) {
            String msg = "CreateLogicalEdgeFirewallCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Create vservice node and update inside port profile for ASA appliance in VSM
     */
    private synchronized Answer execute(ConfigureNexusVsmForAsaCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(ConfigureNexusVsmForAsaCommand cmd, int numRetries) {
    	String vlanId = Long.toString(cmd.getVlanId());
        NetconfHelper helper = null;
        List<Pair<OperationType, String>> params = new ArrayList<Pair<OperationType, String>>();
        params.add(new Pair<OperationType, String>(OperationType.addvlanid, vlanId));
        try {
            helper = new NetconfHelper(cmd.getVsmIp(), cmd.getVsmUsername(), cmd.getVsmPassword());
            s_logger.debug("Connected to Cisco VSM " + cmd.getVsmIp());
            helper.addVServiceNode(vlanId, cmd.getIpAddress());
            s_logger.debug("Created vservice node for ASA appliance in Cisco VSM for vlan " + vlanId);
            helper.updatePortProfile(cmd.getAsaInPortProfile(), SwitchPortMode.access, params);
            s_logger.debug("Updated inside port profile for ASA appliance in Cisco VSM with new vlan " + vlanId);
        } catch (Throwable e) {
            String msg = "ConfigureVSMForASACommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }
}