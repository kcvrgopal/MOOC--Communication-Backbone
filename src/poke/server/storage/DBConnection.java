package poke.server.storage;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import poke.server.resources.ResourceUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import eye.Comm.GetCourse;
import eye.Comm.JobDesc;
import eye.Comm.JobStatus;
import eye.Comm.NameSpace;
import eye.Comm.NameSpaceStatus;
import eye.Comm.Payload;
import eye.Comm.Ping;
import eye.Comm.PokeStatus;
import eye.Comm.Request;
import eye.Comm.RequestList;
import eye.Comm.SignIn;
import eye.Comm.SignUp;

public class DBConnection {
	MongoClient mc;
	DB database;
	DBCollection coll;

	public DBConnection() {
		try {
			mc = new MongoClient("localhost", 27017);
			database = mc.getDB("test");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Request userSignUp(Request request) {
		BasicDBObject document = new BasicDBObject();
		Request reply;
		Request.Builder rb = Request.newBuilder();
		if (request != null) {
			BasicDBObject findQuery = new BasicDBObject("user_name", request
					.getBody().getSignUp().getUserName());
			coll = database.getCollection("user");
			DBObject userNm = coll.findOne(findQuery);
			if ((userNm == null)) {
				document.put("first_name", request.getBody().getSignUp()
						.getFullName());
				document.put("user_name", request.getBody().getSignUp()
						.getUserName());
				document.put("password", request.getBody().getSignUp()
						.getPassword());
				coll.insert(document);

				// metadata
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.SUCCESS, "User Registerd Successfully"));

				// payload
				Payload.Builder pb = Payload.newBuilder();
				NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
				fb.setStatus(PokeStatus.SUCCESS);
				rb.setBody(pb.build());

				reply = rb.build();
				return reply;
			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.FAILURE, "User Already Exists"));

				// payload
				Payload.Builder pb = Payload.newBuilder();
				NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
				fb.setStatus(PokeStatus.FAILURE);
				rb.setBody(pb.build());
				reply = rb.build();
				return reply;
			}

		} else {
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
					PokeStatus.NORESOURCE, "No Request Came"));
			Payload.Builder pb = Payload.newBuilder();
			NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
			fb.setStatus(PokeStatus.NORESOURCE);
			rb.setBody(pb.build());
			reply = rb.build();
			return reply;
		}

	}

	public Request userSignIn(Request request) {
		Request reply;
		Request.Builder rb = Request.newBuilder();
		if (request != null) {
			BasicDBObject findQuery = new BasicDBObject("user_name", request
					.getBody().getSignIn().getUserName());
			coll = database.getCollection("user");
			DBObject credentials = coll.findOne(findQuery);
			if ((credentials != null)) {
				String password = credentials.get("password").toString();
				if (password
						.equals(request.getBody().getSignIn().getPassword())) {

					rb.setHeader(ResourceUtil.buildHeaderFrom(
							request.getHeader(), PokeStatus.SUCCESS,
							"User logged in successfully"));

					Payload.Builder pb = Payload.newBuilder();
					NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
					fb.setStatus(PokeStatus.SUCCESS);
					rb.setBody(pb.build());

					reply = rb.build();
					return reply;
				} else {
					rb.setHeader(ResourceUtil.buildHeaderFrom(
							request.getHeader(), PokeStatus.FAILURE,
							"Username and password didn't match"));
					Payload.Builder pb = Payload.newBuilder();
					NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
					fb.setStatus(PokeStatus.FAILURE);
					rb.setBody(pb.build());
					reply = rb.build();
					return reply;
				}
			} else {
				rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						PokeStatus.NOFOUND, "Username doesn't exist"));
				Payload.Builder pb = Payload.newBuilder();
				NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
				fb.setStatus(PokeStatus.NOFOUND);
				rb.setBody(pb.build());
				reply = rb.build();
				return reply;
			}
		} else {
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
					PokeStatus.NORESOURCE, "Request is Null"));
			Payload.Builder pb = Payload.newBuilder();
			NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
			fb.setStatus(PokeStatus.NORESOURCE);
			rb.setBody(pb.build());
			reply = rb.build();
			return reply;
		}
	}

	public Request ListCourses(Request request) {
		Request reply;
		int count = 0;
		Request.Builder rb = Request.newBuilder();
		RequestList.Builder rl = RequestList.newBuilder();
		GetCourse.Builder gc = GetCourse.newBuilder();
		coll = database.getCollection("course");
		DBCursor docs = coll.find();
		while (docs.hasNext()) {
			DBObject doc = docs.next();
			String course_str = doc.get("course_id").toString();
			Double d = Double.valueOf(course_str);
			int c = d.intValue();
			gc.setCourseId(c);
			gc.setCourseName(doc.get("course_name").toString());
			gc.setCourseDescription(doc.get("course_desc").toString());
			rl.addCourseList(gc.build());
			count++;

		}
		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
				PokeStatus.SUCCESS, "Registered List"));

		Payload.Builder pb = Payload.newBuilder();
		NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
		fb.setStatus(PokeStatus.SUCCESS);
		pb.setReqList(rl.build());
		rb.setBody(pb.build());

		reply = rb.build();
		return reply;

	}

	public Request ListCourse(Request request) {
		Request reply;
		int count = 0;
		Request.Builder rb = Request.newBuilder();
		RequestList.Builder rl = RequestList.newBuilder();
		GetCourse.Builder gc = GetCourse.newBuilder();
		BasicDBObject findQuery = new BasicDBObject("course_id", request
				.getBody().getGetCourse().getCourseId());
		coll = database.getCollection("course");
		DBCursor docs = coll.find(findQuery);
		while (docs.hasNext()) {
			DBObject doc = docs.next();
			String course_str = doc.get("course_id").toString();
			Double d = Double.valueOf(course_str);
			int c = d.intValue();
			gc.setCourseId(c);
			gc.setCourseName(doc.get("course_name").toString());
			gc.setCourseDescription(doc.get("course_desc").toString());
			rl.addCourseList(gc.build());
			count++;

		}
		rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
				PokeStatus.SUCCESS, "Registered List"));

		Payload.Builder pb = Payload.newBuilder();
		NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
		fb.setStatus(PokeStatus.SUCCESS);
		pb.setReqList(rl.build());
		rb.setBody(pb.build());
		reply = rb.build();
		return reply;
	}

	public Request ListCourseByName(Request request) {
		Request reply;
		int count = 0;
		Request.Builder rb = Request.newBuilder();
		RequestList.Builder rl = RequestList.newBuilder();
		GetCourse.Builder gc = GetCourse.newBuilder();
		BasicDBObject findQuery = new BasicDBObject("course_id", request
				.getBody().getGetCourse().getCourseId());
		coll = database.getCollection("course");
		DBCursor docs = coll.find(findQuery);
		if (docs.size() > 0) {
			while (docs.hasNext()) {
				DBObject doc = docs.next();
				String course_str = doc.get("course_id").toString();
				Double d = Double.valueOf(course_str);
				int c = d.intValue();
				gc.setCourseId(c);
				gc.setCourseName(doc.get("course_name").toString());
				gc.setCourseDescription(doc.get("course_desc").toString());
				rl.addCourseList(gc.build());
				count++;

			}
			rb.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
					PokeStatus.SUCCESS, "Requested Course"));
			Payload.Builder pb = Payload.newBuilder();
			NameSpaceStatus.Builder fb = NameSpaceStatus.newBuilder();
			fb.setStatus(PokeStatus.SUCCESS);
			pb.setReqList(rl.build());
			rb.setBody(pb.build());
			reply = rb.build();
			return reply;
		} else {
			// TODO forward the request to nearest server
			return null;
		}

	}

}
