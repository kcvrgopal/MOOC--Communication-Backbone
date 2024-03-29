/*
 * copyright 2013, gash
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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eye.Comm;
import eye.Comm.JobBid;
import eye.Comm.Management;
import poke.monitor.JobBidMonitor;
import poke.monitor.MonitorListener;
import poke.server.conf.NodeDesc;
import poke.server.management.ManagementQueue;

public class JobBidListener implements MonitorListener {
	NodeDesc data;
	protected static Logger logger = LoggerFactory.getLogger("management");

	public JobBidListener(NodeDesc data) {
		this.data = data;
	}

	public NodeDesc getData() {
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#getListenerID()
	 */
	@Override
	public String getListenerID() {
		return data.getNodeId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.monitor.MonitorListener#onMessage(eye.Comm.Management)
	 */
	@Override
	public void onMessage(eye.Comm.Management msg) {
		if (logger.isDebugEnabled())
			logger.debug(msg.getElection().getNodeId());
		if (msg.hasJobPropose()) {
			Comm.JobBid.Builder jobBid = JobBid.getDefaultInstance()
					.newBuilder();
			jobBid.setBid(msg.getJobPropose().getWeight());
			jobBid.setJobId(msg.getJobPropose().getJobId());
			jobBid.setNameSpace(msg.getJobPropose().getNameSpace());
			jobBid.setOwnerId(msg.getJobPropose().getOwnerId());
			Comm.Management.Builder management = Management
					.getDefaultInstance().newBuilder();
			management.build();
			JobBidMonitor em = new JobBidMonitor(data.getNodeId(),
					data.getHost(), data.getMgmtPort());
			SocketAddress sa = new InetSocketAddress(data.getHost(),
					data.getMgmtPort());
			ManagementQueue
					.enqueueRequest(management.build(), em.connect(), sa);
		} else if (msg.hasJobBid()) {
			JobBidMonitor em = new JobBidMonitor(data.getNodeId(),
					data.getHost(), data.getMgmtPort());
			SocketAddress sa = new InetSocketAddress(data.getHost(),
					data.getMgmtPort());
			ManagementQueue.enqueueRequest(msg, em.connect(), sa);
		} else
			logger.error("Received JobBidMgr from on wrong channel or unknown host: "
					+ msg.getBeat().getNodeId());
	}

	@Override
	public void connectionClosed() {
		// note a closed management port is likely to indicate the primary port
		// has failed as well
	}

	@Override
	public void connectionReady() {
		// do nothing at the moment
	}
}
