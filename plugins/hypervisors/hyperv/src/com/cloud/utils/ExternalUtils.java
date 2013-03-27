package com.cloud.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.trilead.ssh2.ChannelCondition;

public class ExternalUtils {

	private static final Logger s_logger = Logger.getLogger(ExternalUtils.class);
	private static final int DEFAULT_DOMR_SSHPORT = 3922;
	private static final int DEFAULT_SSHPORT = 22;
	private static final long _ops_timeout = 150000; // 2.5 minutes
	private static final int _retry = 24;


	public static void registerLocalDhcpEntry(String vmMac, String vmIpAddress, String vmName)
	{
		commandExecute(constructDhcpEntryCommand(vmMac, vmIpAddress, vmName));
	}

	public static void registerDhcpEntry(String vmMac, String vmIpAddress,
			String vmName, String ip, int port) throws Exception {
		registerDhcpEntry(vmMac, vmIpAddress, vmName, ip, port,
				getSystemVMKeyFile(), null);
	}

	public static void registerDhcpEntry(String vmMac, String vmIpAddress,
			String vmName, String ip, int port, String password)
	throws Exception {
		registerDhcpEntry(vmMac, vmIpAddress, vmName, ip, port, null, password);
	}

	public static void registerDhcpEntry(String vmMac, String vmIpAddress,
			String vmName, String ip, int port, File keyFile, String password)
	throws Exception {
		if (ip == null)
			return;

		String command = constructDhcpEntryCommand(vmMac, vmIpAddress, vmName);

		s_logger.debug("Run command on domR " + ip + " "  + command);

		Pair<Boolean, String> result = sshExecute(ip, port, "root",
				keyFile, password, command);

		if (!result.first()) {
			s_logger.error("dhcp_entry command on domR " + ip
					+ " failed, message: " + result.second());

			throw new Exception("DhcpEntry failed due to " + result.second());
		}

		s_logger.info("dhcp_entry command on domain router " + ip
				+ " completed");

	}

	public static String connect(final String ipAddress) {
		return connect(ipAddress, DEFAULT_SSHPORT);
	}

	public static String connect(final String ipAddress, final int port) {
		long startTick = System.currentTimeMillis();

		// wait until we have at least been waiting for _ops_timeout time or
		// at least have tried _retry times, this is to coordinate with system
		// VM patching/rebooting time that may need
		int retry = _retry;
		while (System.currentTimeMillis() - startTick <= _ops_timeout
				|| --retry > 0) {
			SocketChannel sch = null;
			try {
				s_logger.info("Trying to connect to " + ipAddress);
				sch = SocketChannel.open();
				sch.configureBlocking(true);
				sch.socket().setSoTimeout(5000);

				InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
				sch.connect(addr);
				return null;
			} catch (IOException e) {
				s_logger.debug("Could not connect to " + ipAddress + " due to "
						+ e.toString());
				if (e instanceof ConnectException) {
					// if connection is refused because of VM is being started,
					// we give it more sleep time
					// to avoid running out of retry quota too quickly
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
			} finally {
				if (sch != null) {
					try {
						sch.close();
					} catch (IOException e) {
					}
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
			}
		}

		s_logger.info("Unable to logon to " + ipAddress);

		return "Unable to connect";
	}

	public static File getSystemVMKeyFile() {
		URL url = ExternalUtils.class.getProtectionDomain().getCodeSource().getLocation();
		File file = new File(url.getFile());

		File keyFile = new File(file.getParent(), "/scripts/vm/systemvm/id_rsa.cloud");
		if (!keyFile.exists()) {
			keyFile = new File("/usr/lib64/cloud/agent" + "/scripts/vm/systemvm/id_rsa.cloud");
			if (!keyFile.exists()) {
				keyFile = new File("/usr/lib/cloud/agent" + "/scripts/vm/systemvm/id_rsa.cloud");
			}
		}
		return keyFile;
	}

	private static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command) 
	throws Exception {

		return sshExecute(host, port, user, pemKeyFile, password, command, 60000, 60000, 120000);
	}

	public static Pair<Boolean, String> commandExecute(String command) {

		try {
			Process p=Runtime.getRuntime().exec(command);
			p.waitFor();

			StringBuffer sbResult = new StringBuffer();
			int currentReadBytes;
			byte[] buffer = new byte[8192];

			currentReadBytes = p.getInputStream().read(buffer);
			sbResult.append(new String(buffer, 0, currentReadBytes));

			return new Pair<Boolean, String>(true, sbResult.toString());
		}catch (Exception e) {
			return new Pair<Boolean, String>(false, e.getMessage());
		}
	}

	private static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command, 
			int connectTimeoutInMs, int kexTimeoutInMs, int waitResultTimeoutInMs) throws Exception {

		com.trilead.ssh2.Connection conn = null;
		com.trilead.ssh2.Session sess = null; 

		try {
			conn = new com.trilead.ssh2.Connection(host, port);
			conn.connect(null, connectTimeoutInMs, kexTimeoutInMs);

			if(pemKeyFile == null) {
				if(!conn.authenticateWithPassword(user, password)) {
					String msg = "Failed to authentication SSH user " + user + " on host " + host;
					s_logger.error(msg);
					throw new Exception(msg);
				}
			} else {
				if(!conn.authenticateWithPublicKey(user, pemKeyFile, password)) {
					String msg = "Failed to authentication SSH user " + user + " on host " + host;
					s_logger.error(msg);
					throw new Exception(msg);
				}
			}
			sess = conn.openSession();

			// There is a bug in Trilead library, wait a second before
			// starting a shell and executing commands, from http://spci.st.ewi.tudelft.nl/chiron/xref/nl/tudelft/swerl/util/SSHConnection.html
			Thread.sleep(1000);

			sess.execCommand(command);

			InputStream stdout = sess.getStdout();
			InputStream stderr = sess.getStderr();

			byte[] buffer = new byte[8192];
			StringBuffer sbResult = new StringBuffer();

			int currentReadBytes = 0;
			while (true) {
				if ((stdout.available() == 0) && (stderr.available() == 0)) {
					int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 
							waitResultTimeoutInMs);

					if ((conditions & ChannelCondition.TIMEOUT) != 0) {
						String msg = "Timed out in waiting SSH execution result";
						s_logger.error(msg);
						throw new Exception(msg);
					}

					if ((conditions & ChannelCondition.EOF) != 0) {
						if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {                            
							break;
						}
					}
				}

				while (stdout.available() > 0) {
					currentReadBytes = stdout.read(buffer);
					sbResult.append(new String(buffer, 0, currentReadBytes));
				}

				while (stderr.available() > 0) {
					currentReadBytes = stderr.read(buffer);
					sbResult.append(new String(buffer, 0, currentReadBytes));
				}
			}

			String result = sbResult.toString();
			if (sess.getExitStatus() != null && sess.getExitStatus().intValue() != 0 && sess.getExitStatus().intValue() != 1 ) {
				s_logger.error("SSH execution of command " + command + " has an error status code in return. result output: " + result);
				return new Pair<Boolean, String>(false, result);
			}

			return new Pair<Boolean, String>(true, result);
		} finally {
			if(sess != null)
				sess.close();

			if(conn != null)
				conn.close();
		}

	}
	
	private static String constructDhcpEntryCommand(String vmMac, String vmIpAddress, String vmName)
	{
		StringBuffer buff = new StringBuffer();
		
		buff.append("/root/edithosts.sh ")
			.append(vmMac).append(" ")
			.append(vmIpAddress).append(" ")
			.append(vmName);
		
		return buff.toString();
	}
}

