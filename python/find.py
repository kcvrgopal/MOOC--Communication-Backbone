from SocketChannel import SocketChannel, SocketChannelFactory
from comm_pb2 import Request, Response, Document, Payload, Header
import os 
import sys 

class Client():
	_ORIGINATOR = 'zero'
	
	def __init__(self, host, port):
		self.__host = host
		self.__port = port
		self.__type = "REQUEST"
		self.channelFactory = SocketChannelFactory()
		self.channel = ""
		



	def createRequest(self, filename, target):
		document = Document()
		
	
	
		
		document.docName = os.path.basename(filename)
		
		
		payload = Payload()
		payload.doc.docName = document.docName
		
			
		header = Header()
		header.originator = Client._ORIGINATOR
	
		header.routing_id = 21
		header.remainingHopCount=4
		
		request = Request()
		request.header.originator = header.originator
		
		request.header.routing_id = header.routing_id
		request.header.remainingHopCount=header.remainingHopCount
		
		request.body.doc.docName = payload.doc.docName
		#request.body.doc.chunkContent = payload.doc.chunkContent
		#request.body.chunkContent = payload.chunkContent
		
		return request

	def runChannel(self, filename):
		self.channel = self.channelFactory.openChannel(self.__host, self.__port)

		while(True):
			if(self.__type == "REQUEST"):
				request = self.createRequest(filename)
		
				self.__type = "RESPONSE"
			elif(self.__type == "RESPONSE"):
				response = Response()
				response.ParseFromString(self.channel.read())
				print response
				self.__type = "REQUEST"
				break
				
if(__name__=="__main__"):
	filename= sys.argv[1]


	Client('localhost', 5570).runChannel(filename)
