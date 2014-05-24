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
package poke.server.management.managers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.ElectionMonitor;
import poke.resources.ForwardResource;
import poke.server.ServerInitializer;
import poke.server.conf.LeaderDesc;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.management.ManagementQueue;
import eye.Comm;
import eye.Comm.LeaderElection;
import eye.Comm.Management;
import eye.Comm.LeaderElection.VoteAction;

/**
 * The election manager is used to determine leadership within the network.
 * 
 * @author gash
 * 
 */
public class ElectionManager {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();
	private ConcurrentLinkedQueue<ElectionMonitor> monitor = new ConcurrentLinkedQueue<ElectionMonitor>();
	ForwardResource nextResource;
	ServerConf conf;
	private String nodeId;

	/** @brief the number of votes this server can cast */
	private int votes = 1;

	public static ElectionManager getInstance(String id, int votes) {
		instance.compareAndSet(null, new ElectionManager(id, votes));
		return instance.get();
	}

	public static ElectionManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the manager for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected ElectionManager(String nodeId, int votes) {
		this.nodeId = nodeId;
		if (votes >= 0)
			this.votes = votes;
	}

	/**
	 * @param conf
	 * @param args
	 */
	public void startElection(LeaderElection req, ServerConf conf) {
		this.conf = conf;
		processRequest(req);
	}

	public void processRequest(LeaderElection req) {
		if (req == null)
			return;

		if (req.hasExpires()) {
			long ct = System.currentTimeMillis();
			if (ct > req.getExpires()) {
				// election is over
				return;
			}
		}

		if (req.getVote().getNumber() == VoteAction.ELECTION_VALUE) {
			// an election is declared!
			generateElection(req.getNodeId(), VoteAction.NOMINATE);

		} else if (req.getVote().getNumber() == VoteAction.DECLAREVOID_VALUE) {
			// no one was elected, I am dropping into standby mode`
		} else if (req.getVote().getNumber() == VoteAction.DECLAREWINNER_VALUE) {
			// some node declared themself the leader

			if (!req.getNodeId().equalsIgnoreCase(nodeId))
				generateElection(req.getNodeId(), VoteAction.DECLAREWINNER);
			LeaderDesc ld = new LeaderDesc();
			ld.setHost_Leader(req.getHostLeader());
			ld.setPort_Leader(req.getPortLeader());
			logger.info("\n*******************************************\n***************************************\n"
						+"The Leader is   :" + req.getNodeId()+
						"\n*******************************************\n***************************************\n");

		} else if (req.getVote().getNumber() == VoteAction.ABSTAIN_VALUE) {
			// for some reason, I decline to vote
		} else if (req.getVote().getNumber() == VoteAction.NOMINATE_VALUE) {
			int current_node = convertNodeStrToInt(nodeId);
			int nearest_node = convertNodeStrToInt(req.getNodeId());
			// int comparedToMe = req.getNodeId().compareTo(nodeId);
			int comparedToMe = current_node - nearest_node;
			if (comparedToMe > 0) {
				generateElection(nodeId, VoteAction.NOMINATE);
			} else if (comparedToMe < 0) {
				generateElection(req.getNodeId(), VoteAction.NOMINATE);
			} else {
				generateElection(req.getNodeId(), VoteAction.DECLAREWINNER);
			}
		}
	}

	//Performs Leader Election
	public void generateElection(String node_id,
			Comm.LeaderElection.VoteAction voteType) {
		for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) {
			Comm.LeaderElection.Builder electionBuilder = LeaderElection
					.getDefaultInstance().newBuilder();
			electionBuilder.setNodeId(node_id);
			electionBuilder.setVote(voteType);
			electionBuilder.setDesc("Leader Election");
			electionBuilder.setBallotId(node_id);
			electionBuilder.setHostLeader(conf.getServer().getProperty("host"));
			electionBuilder.setPortLeader(conf.getServer().getProperty("port"));

			LeaderElection leaderElection = electionBuilder.build();
			Comm.Management.Builder managementBuilder = Management
					.getDefaultInstance().newBuilder();
			managementBuilder.setElection(leaderElection);
			Management management = managementBuilder.build();

			ElectionMonitor em = new ElectionMonitor(nn.getNodeId(),
					nn.getHost(), nn.getMgmtPort());
			em.startElection(management);
		}
	}
//Method to process Numbers in words to digits
	private int convertNodeStrToInt(String nodeId2) {
		int num = -1;
		if (nodeId2.equalsIgnoreCase("zero"))
			num = 0;
		else if (nodeId2.equalsIgnoreCase("one"))
			num = 1;
		else if (nodeId2.equalsIgnoreCase("two"))
			num = 2;
		else if (nodeId2.equalsIgnoreCase("three"))
			num = 3;
		else if (nodeId2.equalsIgnoreCase("four"))
			num = 4;
		else if (nodeId2.equalsIgnoreCase("five"))
			num = 5;
		else if (nodeId2.equalsIgnoreCase("six"))
			num = 6;

		return num;
	}
}
