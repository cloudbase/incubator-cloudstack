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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import com.cloud.api.response.F5LoadBalancerResponse;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.F5ExternalLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listF5LoadBalancers", responseObject=F5LoadBalancerResponse.class, description="lists F5 load balancer devices")
public class ListF5LoadBalancersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListF5LoadBalancersCmd.class.getName());
    private static final String s_name = "listf5loadbalancerresponse";
    @Inject F5ExternalLoadBalancerElementService _f5DeviceManagerService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_ID, type=CommandType.UUID, entityType = F5LoadBalancerResponse.class,
            description="f5 load balancer device ID")
    private Long lbDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getLoadBalancerDeviceId() {
        return lbDeviceId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<ExternalLoadBalancerDeviceVO> lbDevices = _f5DeviceManagerService.listF5LoadBalancers(this);
            ListResponse<F5LoadBalancerResponse> response = new ListResponse<F5LoadBalancerResponse>();
            List<F5LoadBalancerResponse> lbDevicesResponse = new ArrayList<F5LoadBalancerResponse>();

            if (lbDevices != null && !lbDevices.isEmpty()) {
                for (ExternalLoadBalancerDeviceVO lbDeviceVO : lbDevices) {
                    F5LoadBalancerResponse lbdeviceResponse = _f5DeviceManagerService.createF5LoadBalancerResponse(lbDeviceVO);
                    lbDevicesResponse.add(lbdeviceResponse);
                }
            }

            response.setResponses(lbDevicesResponse);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
