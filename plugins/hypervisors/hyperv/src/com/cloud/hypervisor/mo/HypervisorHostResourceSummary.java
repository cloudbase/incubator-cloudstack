package com.cloud.hypervisor.mo;

public class HypervisorHostResourceSummary {
	private long memoryBytes;
	private long cpuCount;
	private long cpuSpeed;

	public HypervisorHostResourceSummary() {
	}
	
	public long getMemoryBytes() {
		return memoryBytes;
	}
	
	public void setMemoryBytes(long memoryBytes) {
		this.memoryBytes = memoryBytes;
	}
	
	public long getCpuCount() {
		return cpuCount;
	}
	
	public void setCpuCount(long cpuCount) {
		this.cpuCount = cpuCount;
	}
	
	public long getCpuSpeed() {
		return cpuSpeed;
	}
	
	public void setCpuSpeed(long cpuSpeed) {
		this.cpuSpeed = cpuSpeed;
	}
}