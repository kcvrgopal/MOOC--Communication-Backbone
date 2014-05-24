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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.ServerInitializer;
import eye.Comm.Request;

/**
 
 * 
 * @author gash
 * 
 */
public class ResourceMonitor {
	protected static Logger logger = LoggerFactory.getLogger("mgmt");

	protected ChannelFuture channel; // do not use directly, call connect()!
	private EventLoopGroup group;

	private String host;
	private int port;

	
	public ResourceMonitor(String host, String port) {
		this.host = host;
		this.port = Integer.parseInt(port);
		this.group = new NioEventLoopGroup();
	}
	public ResourceMonitor(String host, int port) {
		this.host = host;
		this.port = port;
		this.group = new NioEventLoopGroup();
	}
	
	

	

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	public Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			try {
				
				Bootstrap b = new Bootstrap();
				// @TODO newFixedThreadPool(2);
				b.group(group).channel(NioSocketChannel.class).handler(new ServerInitializer(false));
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// Make the connection attempt.
				logger.info(host+":"+port+"IN Resource monitor");
				channel = b.connect(host, port).syncUninterruptibly();
				channel.awaitUninterruptibly(5000l);
				
			} catch (Exception ex) {
				logger.debug("failed to initialize the heartbeat connection");

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
	 * request the node to send heartbeats.
	 * @param management 
	 * 
	 * @return did a connect and message succeed
	 */
	public boolean startFindLeader(Request request) {
	
		boolean rtn = false;
		try {
			Channel ch = connect();			
			ch.writeAndFlush(request);
			rtn = true;
			} catch (Exception e) {
				logger.error("could not send connect to node", e);
		}
		return rtn;
	}

	
	public void waitForever() {
		try {
			boolean connected = true;
			while (connected) {
				Thread.sleep(2000);
			}
			logger.info("---> trying to connect external Node");
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
	//Method to start voting
	public void startVoting(Request request) {
		boolean rtn = false;
		try {
			Channel ch = connect();			
			ch.writeAndFlush(request);
			rtn = true;
			} catch (Exception e) {
				logger.error("could not send connect to node", e);
		}
		
	}


	
}
