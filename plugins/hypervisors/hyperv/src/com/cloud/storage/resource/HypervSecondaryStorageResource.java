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
package com.cloud.storage.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.host.Host.Type;
import com.cloud.storage.Storage.StorageResourceType;
import com.cloud.utils.script.Script;

public class HypervSecondaryStorageResource extends NfsSecondaryStorageResource implements SecondaryStorageResourceHandler {

    private static final Logger s_logger = Logger.getLogger(HypervSecondaryStorageResource.class);

    private Map<String, String> _activeOutgoingAddresses = new HashMap<String, String>();
	
    /*
    @Override
    public Answer executeRequest(Command cmd) {
    	if (cmd instanceof ListTemplateCommand){
            return execute((ListTemplateCommand)cmd);
        }

        return defaultAction(cmd);
    }
    
    
    
    public Answer defaultAction(Command cmd) {
    	return super.executeRequest(cmd);
    }
    
    private Answer execute(ListTemplateCommand cmd) {
        if (cmd.getSwift() != null) {
            Map<String, TemplateInfo> templateInfos = swiftListTemplate(cmd.getSwift());
            return new ListTemplateAnswer(cmd.getSwift().toString(), templateInfos);
        } else {
            String root = getRootDir(cmd.getSecUrl());
            Map<String, TemplateInfo> templateInfos = _dlMgr.gatherTemplateInfo(root);
            return new ListTemplateAnswer(cmd.getSecUrl(), templateInfos);
        }
    }
    */
    
    public void ensureOutgoingRuleForAddress(String address) {
    	if(address == null || address.isEmpty() || address.startsWith("0.0.0.0")) {
    		if(s_logger.isInfoEnabled())
    			s_logger.info("Drop invalid dynamic route/firewall entry " + address);
    		return;
    	}
    	
    	boolean needToSetRule = false;
    	synchronized(_activeOutgoingAddresses) {
    		if(!_activeOutgoingAddresses.containsKey(address)) {
    			_activeOutgoingAddresses.put(address, address);
    			needToSetRule = true;
    		}
    	}
    	
    	if(needToSetRule) {
    		if(s_logger.isInfoEnabled())
    			s_logger.info("Add dynamic route/firewall entry for " + address);
    		allowOutgoingOnPrivate(address);
    	}
    }
    
    @Override
    public String getRootDir(String secUrl) {
        if(!(secUrl.startsWith("http") || secUrl.startsWith("nfs")))
        	secUrl = "http://" + secUrl;
        
        return super.getRootDir(secUrl);
    }
    
    @Override
    public StartupCommand[] initialize() {
        
        final StartupStorageCommand cmd = new StartupStorageCommand();
        fillNetworkInformation(cmd);
        if(_publicIp != null)
            cmd.setPublicIpAddress(_publicIp);
        
        cmd.setName(_name);
        cmd.setDataCenter(_dc);
        cmd.setGuid(_guid);
        
        cmd.setPrivateIpAddress(_eth1ip);
        cmd.setPrivateNetmask(_eth1mask);
        cmd.setPrivateMacAddress("255.255.255.0");
        cmd.setPublicIpAddress(_publicIp);
        //cmd.setPublicMacAddress(publicMacAddress);
        //cmd.setPublicNetmask(_eth1mask);
        cmd.setStorageIpAddress(_storageIp);
        //cmd.setStorageMacAddress(storageMacAddress);
        cmd.setStorageNetmask(_storageNetmask);
        cmd.setVersion("NFS");
        cmd.setIqn(_guid);
        
        cmd.setResourceType(StorageResourceType.SECONDARY_STORAGE);
        cmd.setHostType(Type.SecondaryStorageVM);
        
        Script command = new Script("/bin/bash", s_logger);
        command.add("-c");
        command.add("ln -sf " + _parent + " /var/www/html/copy");
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Error in linking  err=" + result);
            return null;
        }
        return new StartupCommand[] {cmd};
    }
    
      
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	super.configure(name, params);
    	
    	_name = _guid;
    	_inSystemVM = true;

    	return true;
    }
}
