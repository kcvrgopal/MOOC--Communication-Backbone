/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.monitor;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.management.ManagementHandler;
import poke.server.management.managers.ElectionManager;
import eye.Comm.Management;
import eye.Comm.Network;
import eye.Comm.Network.NetworkAction;

/**
 * 
 * @author gash
 * 
 */
public class JobBidMonitor {
	protected static Logger logger = LoggerFactory.getLogger("mgmt");

	protected ChannelFuture channel; // do not use directly, call connect()!
	private EventLoopGroup group;

	private static int N = 0;
	private String whoami;
	private String host;
	private int port;

	// this list is only used if the connection cannot be established - it holds
	// the listeners to be added.
	private List<MonitorListener> listeners = new ArrayList<MonitorListener>();

	private MonitorHandler handler;
	//private ManagementHandler handler;
	private int flag=1;

	/**
	 * Create a heartbeat message processor.
	 * 
	 * @param host
	 *            the hostname
	 * @param port
	 *            This is the management port
	 */
	public JobBidMonitor(String whoami, String host, int port) {
		this.whoami = whoami;
		this.host = host;
		this.port = port;
		this.group = new NioEventLoopGroup();
	}

	public MonitorHandler getHandler() {
		return handler;
	}

	/**
	 * abstraction of notification in the communication
	 * 
	 * @param listener
	 */
	public void addListener(MonitorListener listener) {
		if (handler == null && !listeners.contains(listener)) {
			listeners.add(listener);
			return;
		}

		try {
			handler.addListener(listener);
		} catch (Exception e) {
			logger.error("failed to add listener", e);
		}
	}

	public void release() {
		logger.warn("HeartMonitor releasing resources");

		for (String id : handler.listeners.keySet()) {
			MonitorListener ml = handler.listeners.get(id);
			ml.connectionClosed();

			// hold back listeners to re-apply if the connection is
			// re-established.
			listeners.add(ml);
		}

		// TODO should wait a fixed time and use a listener to reset values;s
		channel = null;
		handler = null;
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	public Channel connect() {
		// Start the connection attempt.
	//	while(true){
		if (channel == null) {
			try {
				//if(flag==1)
				//{
				handler = new MonitorHandler();
				MonitorInitializer mi = new MonitorInitializer(handler, false);

				Bootstrap b = new Bootstrap();
				// @TODO newFixedThreadPool(2);
				b.group(group).channel(NioSocketChannel.class).handler(mi);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// Make the connection attempt.
				logger.info(host+";"+port+"IN JobBid monitor");
				channel = b.connect(host, port).syncUninterruptibly();
				channel.awaitUninterruptibly(5000l);
				channel.channel().closeFuture().addListener(new MonitorClosedListener(this));
				//flag=0;
				//Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
				if (N == Integer.MAX_VALUE)
					N = 1;
				else
					N++;
				//}
				// add listeners waiting to be added
				if (listeners.size() > 0) {
					for (MonitorListener ml : listeners)
						handler.addListener(ml);
					listeners.clear();
				}
			} catch (Exception ex) {
				logger.debug("failed to initialize the heartbeat connection");
				// logger.error("failed to initialize the heartbeat connection",
				// ex);
			}
		}
		

		if (channel != null && channel.isDone() && channel.isSuccess()){
			
			
			logger.info("Channel is Created"+channel);
			return channel.channel();
		}
		else
		{
			logger.info("In Error from Establish Connection");
			throw new RuntimeException("Not able to establish connection to server");
		}

	
		}

	
	//}
	public boolean isConnected() {
		if (channel == null)
			return false;
		else
			return channel.channel().isOpen();
	}

	public String getNodeInfo() {
		if (host != null)
			return host + ":" + port;
		else
			return "Unknown";
	}

	/**
	 * request the node to send jobProposal.
	 * @param management 
	 * 
	 * @return did a connect and message succeed
	 */
	public boolean startJobBid(Management management) {
		// the join will initiate the other node's heartbeatMgr to reply to
		// this node's (caller) listeners.

		boolean rtn = false;
		try {
			Channel ch = connect();
			Management.Builder m = Management.newBuilder();
			ch.writeAndFlush(management);
			rtn = true;
			logger.info("after jobBid manager in jobBid Manager");			
			} catch (Exception e) {
				logger.error("could not send connect to node", e);
		}
		return rtn;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	public void waitForever() {
		try {
			boolean connected = true;
			while (connected) {
				Thread.sleep(2000);
			}
			logger.info("---> trying to connect heartbeat");
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	public static class MonitorClosedListener implements ChannelFutureListener {
		private JobBidMonitor monitor;

		public MonitorClosedListener(JobBidMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			monitor.release();
		}
	}
}
