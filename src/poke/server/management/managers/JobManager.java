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

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.JobBidMonitor;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import eye.Comm;
import eye.Comm.JobBid;
import eye.Comm.JobProposal;
import eye.Comm.Management;

/**
 * The job manager class is used by the system to assess and vote on a job. This
 * is used to ensure leveling of the servers take into account the diversity of
 * the network.
 * 
 * @author gash
 * 
 */
public class JobManager {
	protected static Logger logger = LoggerFactory.getLogger("management");
	protected static AtomicReference<JobManager> instance = new AtomicReference<JobManager>();

	public String nodeId;
	private static ServerConf conf;
	public static int myWeight;
	public static String won_node = "zero";

	public static JobManager getInstance(String id, ServerConf conf) {
		instance.compareAndSet(null, new JobManager(id));
		JobManager.conf = conf;
		return instance.get();
	}

	public static JobManager getInstance() {
		return instance.get();
	}

	public JobManager(String nodeId) {
		this.nodeId = nodeId;
	}

	public JobManager(ServerConf conf) {
		this.conf = conf;
	}

	/**
	 * a new job proposal has been sent out that I need to evaluate if I can run
	 * it
	 * 
	 * @param req
	 *            The proposal
	 */

	public void processRequest(JobProposal req) {
		for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) {
			// Job Bid object created
			Comm.JobBid.Builder jobBid = JobBid.getDefaultInstance()
					.newBuilder();
			jobBid.setBid(myWeight);
			jobBid.setJobId(req.getJobId());
			jobBid.setNameSpace(req.getNameSpace());
			jobBid.setOwnerId(convertNodeStrToInt(conf.getServer().getProperty(
					"node.id")));
			Comm.Management.Builder management = Management
					.getDefaultInstance().newBuilder();
			management.setJobBid(jobBid.build());
			management.build();
			JobBidMonitor jobMonitor = new JobBidMonitor(nn.getNodeId(),
					nn.getHost(), nn.getMgmtPort());
			jobMonitor.startJobBid(management.build());
		}
	}

	/**
	 * a job bid for my job
	 * 
	 * @param req
	 *            The bid
	 */
	// Calling the job bid object
	public void processRequest(JobBid req) {
		if (req.getNameSpace().equals(conf.getServer().getProperty("node.id"))) {
			// Owner id of the node which initiates the bidding
			System.out.println("\n******************************************************\n"+
			"The node that can process the request is : "+ req.getOwnerId()+
			"\n*******************************************************");
			JobManager.won_node = changeToStr(req.getOwnerId());
		} else {
			if (req.getBid() <= myWeight) {
				for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) {
					Comm.Management.Builder management = Management
							.getDefaultInstance().newBuilder();
					management.setJobBid(req);
					management.build();
					JobBidMonitor jobMonitor = new JobBidMonitor(
							nn.getNodeId(), nn.getHost(), nn.getMgmtPort());
					jobMonitor.startJobBid(management.build());
				}
			} else if (req.getBid() > myWeight) {
				for (NodeDesc nn : conf.getNearest().getNearestNodes().values()) {
					Comm.JobBid.Builder jobBid = JobBid.getDefaultInstance()
							.newBuilder();
					jobBid.setBid(myWeight);
					jobBid.setJobId(req.getJobId());
					jobBid.setNameSpace(req.getNameSpace());
					jobBid.setOwnerId(convertNodeStrToInt(conf.getServer()
							.getProperty("node.id")));
					Comm.Management.Builder management = Management
							.getDefaultInstance().newBuilder();
					management.setJobBid(jobBid.build());
					management.build();
					JobBidMonitor jobMonitor = new JobBidMonitor(
							nn.getNodeId(), nn.getHost(), nn.getMgmtPort());
					jobMonitor.startJobBid(management.build());
				}
			}
		}

	}

	// Convert digits to Numbers in words
	private String changeToStr(long ownerId) {
		// TODO Auto-generated method stub
		switch ((int) ownerId) {
		case 0:
			return "zero";
		case 1:
			return "one";
		case 2:
			return "two";
		case 3:
			return "three";
		case 4:
			return "four";
		case 5:
			return "five";
		default:
			return "zero";
		}
	}

	// Convert number in words to digits
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
