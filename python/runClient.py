from SocketChannel import SocketChannel, SocketChannelFactory
from comm_pb2 import Request,Payload, Header, Ping, SignIn,SignUp,GetCourse,RequestList,InitVoting
from google.protobuf.internal import enum_type_wrapper
import os 
import sys 
import json

class Client():
	_ORIGINATOR = 'Client'
	
	def __init__(self, host, port,__type):
		self.__host = host
		self.__port = port
		self.__type = __type
		self.channelFactory = SocketChannelFactory()
		self.channel = ""
	#function to build protobuf request
	def createRequest(self, msg):
		request = Request()
		payload = Payload()
		ping=Ping()
		header = Header()
		header.routing_id=2
		header.originator=Client._ORIGINATOR
		ping.number=1
		ping.tag=msg
		payload.ping.tag=ping.tag
		payload.ping.number=ping.number
		request.header.originator = header.originator
		request.header.routing_id = header.routing_id
		request.body.ping.tag=payload.ping.tag
		request.body.ping.number=payload.ping.number
		header.originator = Client._ORIGINATOR
		header.routing_id = 11
		return request
	#Creates SignIn request
	def createSign(self,user_name,password):
		request = Request()
		payload = Payload()
		sign_in=SignIn()
		header=Header()
		sign_in.user_name=user_name
		sign_in.password=password
		payload.sign_in.user_name=sign_in.user_name
		payload.sign_in.password=sign_in.password
		header.routing_id=13
		header.originator=Client._ORIGINATOR
		request.body.sign_in.user_name= payload.sign_in.user_name
		request.body.sign_in.password=payload.sign_in.password
		request.header.routing_id=13
		request.header.originator=Client._ORIGINATOR
		return request
	 #Creates SignUp request
	def createSignUp(self,user_name,password,full_name):
		#print 'user'
		request = Request()
		payload = Payload()
		sign_up=SignUp()
		header=Header()
		sign_up.user_name=user_name
		sign_up.password=password
		sign_up.full_name=full_name
		payload.sign_up.user_name=sign_up.user_name
		payload.sign_up.password=sign_up.password
		payload.sign_up.full_name=sign_up.full_name
		header.routing_id=13
		header.originator=Client._ORIGINATOR
		request.body.sign_up.user_name= payload.sign_up.user_name
		request.body.sign_up.password=payload.sign_up.password
		request.body.sign_up.full_name=payload.sign_up.full_name
		request.header.routing_id=13
		request.header.originator=Client._ORIGINATOR
		return request
	#Creates Getting a specific course info request
	def getCourseInfo(self,course_id):
		request=Request()
	 	payload=Payload()
		getcourse=GetCourse()
		header=Header()
		getcourse.course_id=course_id
		payload.get_course.course_id=getcourse.course_id
		header.routing_id=13
		header.originator=Client._ORIGINATOR
		request.body.get_course.course_id=payload.get_course.course_id
		request.header.routing_id=13
		request.header.originator=Client._ORIGINATOR
		return request
	 #Creates Get all Courses request	      
	def getListCourse(self):
		request=Request()
	 	payload=Payload()
		header=Header()
		header.routing_id=13
		header.originator=Client._ORIGINATOR
		request.body.get_course.course_id=-1
		request.header.routing_id=13
		request.header.originator=Client._ORIGINATOR
		return request
	 #Creates HostCompetition request	      
	def hostCompetition(self):
		request = Request()
		payload = Payload()
		header=Header()
		initVoting = InitVoting()
		initVoting.voting_id=""
		request.body.init_voting.voting_id=initVoting.voting_id
		request.header.routing_id=13
		request.header.originator=Client._ORIGINATOR
		return request

	def runChannel(self, msg):
		self.channel = self.channelFactory.openChannel(self.__host, self.__port)
		while(True):
			if(self.__type=="_SIGNIN"):
				request=self.createSign(user_name,password)
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type == "_SIGNUP"):
				request=self.createSignUp(user_name,password,full_name)
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type == "_GETCOURSE"):
				request = self.getCourseInfo(course_id)
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type=="_DISPLAY"):	
				request=self.getListCourse()
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type == "REQUEST"):
				request = self.createRequest(msg)
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type == "_VOTING"):
				request = self.hostCompetition()
				self.channel.write(request.SerializeToString())
				self.__type = "RESPONSE"
			elif(self.__type == "RESPONSE"):
				response = Request()
				response.ParseFromString(self.channel.read())
				print('The response from server is \n')
				print(response)
				self.__type = "REQUEST"
				break
				
if(__name__=="__main__"):
	ipadd=raw_input('Enter The host ip address : ')
	port=int(raw_input('Enter the port number : '))
	print('1.sign in')
	print('2.sign up')
	print('3.Get List')
	print('4.Get Course')
	print('5.HOST COMPETITION')
	choice=int(raw_input('Enter Your Choice:'))
	try:
	    if choice==1:
	      print('choice')
	      user_name=raw_input('Enter Username:')
	      password=raw_input('Enter Password:')
	      __type = "_SIGNIN"
	    elif choice==2:
	      print('choice 2')
	      full_name=raw_input('Enter Full Name:')
	      user_name=raw_input('Enter Username:')
	      password=raw_input('Enter Password:')
	      __type = "_SIGNUP"		
	    elif choice==3:
	      print('3')
	      __type="_DISPLAY"
	    elif choice==4:
	      print('4')
	      course_id=int(raw_input('Enter Course Id:'))
	      __type="_GETCOURSE"
	    elif choice==5:
	      __type="_VOTING"
	except ValueError:
	    print("Not a number")
	#Msg is used to ping before connecting
	msg="Ping"
	Client(ipadd,port,__type).runChannel(msg)
