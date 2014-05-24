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
package poke.resources;

import java.util.ArrayList;
import poke.monitor.LeaderMonitor;
import poke.server.ServerHandler;
import poke.server.conf.LeaderDesc;
import poke.server.resources.Resource;
import poke.server.storage.DBConnection;
import eye.Comm;
import eye.Comm.FindLeader;
import eye.Comm.Header;
import eye.Comm.Header.Routing;
import eye.Comm.InitVoting;
import eye.Comm.Payload;
import eye.Comm.Request;

public class JobResource implements Resource {
	// Processes the request based on the request sent from perchannel queue by
	// client
	@Override
	public Request process(Request request) {
		// TODO Auto-generated method stub
		Request response = null;
		DBConnection dbc = new DBConnection();
		if (request.getBody().hasSignUp()) {
			response = dbc.userSignUp(request);
		} else if (request.getBody().hasSignIn()) {
			response = dbc.userSignIn(request);
		} else if (request.getBody().getGetCourse().getCourseId() == -1) {
			response = dbc.ListCourses(request);
		} else if (request.getBody().hasGetCourse()) {
			response = dbc.ListCourseByName(request);
		} else if (request.getBody().hasInitVoting()) {
			if (request.getHeader().getOriginator().equalsIgnoreCase("client")) {
				// Polls the external clusters
				String host = "192.168.0.";
				String port = "7000";
				for (int i = 1; i < 120; i = i + 10) {
					LeaderMonitor leaderMonitor = new LeaderMonitor(host + i,
							port);
					Comm.FindLeader.Builder findLeader = FindLeader
							.getDefaultInstance().newBuilder();
					Comm.Header.Builder header = Header.getDefaultInstance()
							.toBuilder();
					header.setOriginator("server");
					header.setRoutingId(Routing.JOBS);
					Comm.Payload.Builder payload = Payload.getDefaultInstance()
							.newBuilder();
					payload.setFindLeader(findLeader.build());
					Comm.Request.Builder reqBuilder = Request
							.getDefaultInstance().newBuilder();
					reqBuilder.setBody(payload.build());
					reqBuilder.setHeader(header.build());
					leaderMonitor.startFindLeader(reqBuilder.build());
				}
			} else if (request.getHeader().getOriginator()
					.equalsIgnoreCase("server")) {
				if (request.getBody().getInitVoting().getVotingId().isEmpty()) {
					Comm.InitVoting.Builder voting = InitVoting
							.getDefaultInstance().newBuilder();
					Comm.Payload.Builder payload = Payload.getDefaultInstance()
							.newBuilder();
					voting.setVotingId("1");
					payload.setInitVoting(voting.build());
					Comm.Header.Builder header = Header.getDefaultInstance()
							.toBuilder();
					header.setOriginator("server");
					header.setRoutingId(Routing.JOBS);
					Comm.Request.Builder reqBuilder = Request
							.getDefaultInstance().newBuilder();
					reqBuilder.setBody(payload.build());
					reqBuilder.setHeader(header.build());
					return reqBuilder.build();
				} else {
					System.out.println("VOTING ID FROM SERVER"
							+ request.getBody().getInitVoting().getVotingId());
					ServerHandler.channel.writeAndFlush(request);
					// response=request;
				}
			}
		} else if (request.getBody().hasFindLeader()) {
			if (request.getBody().getFindLeader().getLeaderIp().isEmpty()) {
				Comm.FindLeader.Builder builder = FindLeader
						.getDefaultInstance().newBuilder();
				builder.setLeaderIp(LeaderDesc.getHost_Leader());
				builder.setLeaderPort(LeaderDesc.getPort_Leader());
				Comm.Header.Builder header = Header.getDefaultInstance()
						.toBuilder();
				header.setOriginator("server");
				header.setRoutingId(Routing.JOBS);
				Comm.Payload.Builder payload = Payload.getDefaultInstance()
						.newBuilder();
				payload.setFindLeader(builder.build());
				Comm.Request.Builder reqBuilder = Request.getDefaultInstance()
						.newBuilder();
				reqBuilder.setBody(payload.build());
				reqBuilder.setHeader(header.build());
				return reqBuilder.build();
			} else {
				ArrayList<String> host = new ArrayList<String>();
				// Gets the Leader IP of external clusters
				host.add(request.getBody().getFindLeader().getLeaderIp());
				try {
					Thread.sleep(1000);
					for (int i = 0; i < host.size(); i++) {
						LeaderMonitor leaderMonitor = new LeaderMonitor(
								host.get(i), "7000");
						Comm.InitVoting.Builder voting = InitVoting
								.getDefaultInstance().newBuilder();
						voting.setHostIp(host.get(i));
						voting.setPortIp("7000");
						// Build an protobuf request/response of type payload
						Comm.Payload.Builder payload = Payload
								.getDefaultInstance().newBuilder();
						payload.setInitVoting(voting.build());
						Comm.Header.Builder header = Header
								.getDefaultInstance().toBuilder();
						header.setOriginator("server");
						header.setRoutingId(Routing.JOBS);
						Comm.Request.Builder reqBuilder = Request
								.getDefaultInstance().newBuilder();
						reqBuilder.setBody(payload.build());
						reqBuilder.setHeader(header.build());
						// Entry point for Voting
						leaderMonitor.startVoting(reqBuilder.build());
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
		return response;
	}

}
