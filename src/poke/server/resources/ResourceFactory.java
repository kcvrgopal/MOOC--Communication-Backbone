/*
 * copyright 2012, gash
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
package poke.server.resources;

import java.beans.Beans;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.LeaderMonitor;
import poke.monitor.ResourceMonitor;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.conf.ServerConf.ResourceConf;
import poke.server.management.managers.JobManager;
import eye.Comm;
import eye.Comm.Header;
import eye.Comm.JobProposal;
import eye.Comm.Request;

/**
 * Resource factory provides how the server manages resource creation. We hide
 * the creation of resources to be able to change how instances are managed
 * (created) as different strategies will affect memory and thread isolation. A
 * couple of options are:
 * <p>
 * <ol>
 * <li>instance-per-request - best isolation, worst object reuse and control
 * <li>pool w/ dynamic growth - best object reuse, better isolation (drawback,
 * instances can be dirty), poor resource control
 * <li>fixed pool - favor resource control over throughput (in this case failure
 * due to no space must be handled)
 * </ol>
 * 
 * @author gash
 * 
 */
public class ResourceFactory {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private static ServerConf cfg;
	private static AtomicReference<ResourceFactory> factory = new AtomicReference<ResourceFactory>();

	public static void initialize(ServerConf cfg) {
		try {
			ResourceFactory.cfg = cfg;
			factory.compareAndSet(null, new ResourceFactory());
		} catch (Exception e) {
			logger.error("failed to initialize ResourceFactory", e);
		}
	}

	public static ResourceFactory getInstance() {
		ResourceFactory rf = factory.get();
		if (rf == null)
			throw new RuntimeException("Server not intialized");

		return rf;
	}

	private ResourceFactory() {
	}

	/**
	 * Obtain a resource
	 * 
	 * @param route
	 * @return
	 */
	public Resource resourceInstance(Request request) {
		// is the message for this server?
		if (request.getHeader().hasToNode()) {
			String iam = cfg.getServer().getProperty("node.id");
			if (iam.equalsIgnoreCase(request.getHeader().getToNode()))
				if (request.getBody().hasSignIn()
						|| request.getBody().hasSignUp()) {
					JobManager.myWeight += 2;
				} else {
					JobManager.myWeight += 4;
				}
		} else {
			for (NodeDesc nn : cfg.getNearest().getNearestNodes().values()) {

				ResourceMonitor lm = new ResourceMonitor(nn.getHost(),
						nn.getPort());
				//Finds the leader
				lm.startFindLeader(request);
			}
			System.out.println("Resource Factory---->");

		}

		ResourceConf rc = cfg.findById(request.getHeader().getRoutingId()
				.getNumber());
		if (rc == null)
			return null;

		try {
			// strategy: instance-per-request
			Resource rsc = (Resource) Beans.instantiate(this.getClass()
					.getClassLoader(), rc.getClazz());
			return rsc;
		} catch (Exception e) {
			logger.error("unable to create resource " + rc.getClazz());
			return null;
		}
	}

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
