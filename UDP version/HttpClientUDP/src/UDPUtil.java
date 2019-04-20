import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

class Packet{
	public static final int Buf_Len = 1024; //byte array length
	public static final int Head_Len = 11; //byte length before payload 
	
	public byte type;//the type of the packet. 1-- ACK, 2--DATA, 3--End ACK 4--All packet received, Exit(used for server ACK drop and client exit)
	public int seqNum;// the sequence number of the packet. start from 1
	public InetAddress destAddr;//the destination address of the packet
	public short destPort;//the destination port number
	public String payload;//the real data of the packet . length should less than 1024-11 
	
	public int bytesLength(){
		return payload.length()+Head_Len;
	}

	public Packet(){
		type = 0;//constructor. valid type is 1(ACK),2(DATA),3(End ACK)
	}
	
	public void clone(Packet pk){ //copy Packet
		type = pk.type;
		seqNum = pk.seqNum;
		destAddr=pk.destAddr;
		destPort=pk.destPort;
		payload=pk.payload;
	}
	
	public byte[] getBytes() { //encode class data into a sequence of bytes for UDP Packet 
		byte[] result = new byte[Buf_Len];
		result[0] = type;
		
		result[1] = (byte) ((seqNum >> 24) & 0xFF); //&0xFF: -1 => 255
		result[2] = (byte) ((seqNum >> 16) & 0xFF);
		result[3] = (byte) ((seqNum >> 8) & 0xFF);
		result[4] = (byte) (seqNum & 0xFF);
		
		result[5] =  destAddr.getAddress()[0];
		result[6] =  destAddr.getAddress()[1];
		result[7] =  destAddr.getAddress()[2];
		result[8] =  destAddr.getAddress()[3];

		result[9] = (byte) ((destPort >> 8) & 0xFF);
		result[10] = (byte) (destPort & 0xFF);

		System.arraycopy(payload.getBytes(), 0, result, Head_Len, payload.length()); //payload length should less than 1024-11
		
		return result;
	}
	
	public void setBytes(byte[] data,int length) throws UnknownHostException { //decode a sequence of bytes of UDP Packet into class data  
		type = data[0];	
		
		seqNum = (data[1] & 0xFF) << 24 |
                 (data[2] & 0xFF) << 16 |  
                 (data[3] & 0xFF) << 8 |  		
				 (data[4] & 0xFF);
		
		destAddr = InetAddress.getByAddress(Arrays.copyOfRange(data, 5, 9));
		
		destPort = (short) ((data[9] & 0xFF) << 8 |  		
				            (data[10] & 0xFF));
		
		payload = new String(data, Head_Len, length-Head_Len);
		
	}		
}

class SelectARQ {
	private static final int SendQueue_Size = 5; //sendQueue size: number of Packet to send as one group
	private static final int Max_DataLen = Packet.Buf_Len-Packet.Head_Len;//  1024-11: each Package can contains Max_DataLen payload HttpMessages
	private static final String Time_Out = "2000";//msec.default value
	private static final int Max_Timeout_Try = 5;//if this there is no data, exit loop(other size may exit already) 
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss "); 
  	
	
	public String routerHostCfg; //config parameter in config.ini 
	public int routerPortCfg;
	public String clientHostCfg; //client IP and port can be get from received Packet
	public int clientPortCfg;
	public String serverHostCfg; //server IP and port not needed(can assign by http://serverHost:serverPort)
	public int serverPortCfg;
	public int receiveTimeOut;//should large than router's delay
	
	HashSet<Integer> sendQueue; //sequence number to be sent, when get ACK, will remove from queue
	HashMap<Integer,Packet> sendPackets; //build HttpMessages to Packet before send. Integer is Packet sequence number
	HashMap<Integer,Packet> receivePackets; //keep Packets received to receivePackets
	private int totalPacketNeedReceive;//sent by last Pack using Packet: type=3 and seqNum=len+1
	
	public SelectARQ() {
		sendQueue = new HashSet<Integer>();//for server
		sendPackets= new HashMap<Integer,Packet>();//for server
		receivePackets= new HashMap<Integer,Packet>();//for client
	}
	public int buildSendQueue(int startIndex){  //return end index, minimum index =1
		int result = -1;
		//sendQueue should be empty here (all packets get ACK and removed)
		sendQueue.clear();
		for(int i=0;i<SendQueue_Size;i++)
			if(i+startIndex<=sendPackets.size()){
              result = i+startIndex;
              sendQueue.add(result);
			}
		return result;
	}
	
	public boolean isACKPacket(Packet pk){
		boolean result = (pk.type==1) && sendQueue.contains(pk.seqNum); 
		if(result)
			sendQueue.remove(pk.seqNum); //type=1 is ACK returned, then remove it from queue(remained packet need re-send)
		return result;
	}
	public void buildSendBuf(InetAddress destAddr,short destPort,String payload) { //client's IP & port. payload is http message to send
		sendPackets.clear();
		int len = payload.length()/Max_DataLen+1;
		for(int i = 1; i<=len; i++){
			Packet pk = new Packet();
			pk.type = 2;//2:data
			pk.seqNum = i;
			pk.destAddr = destAddr;
			pk.destPort = destPort;
			int endIndex = i*Max_DataLen <  payload.length() ?i*Max_DataLen:payload.length();
			pk.payload = payload.substring((i-1)*Max_DataLen, endIndex);
					
			sendPackets.put(pk.seqNum, pk);
		}
		//add a end ACK packet
		Packet pk = new Packet();
		//last packet contains sendPackets size information(seqNum = len+1), 
		//because of out-of-order, End ACK may received before data packet,
		//so we can end while loop according to package number received
		pk.type = 3;//3:End ACK .
		pk.seqNum = len+1; //last packet ACK: type=1 and seqNum=sendPackets.size()
		pk.destAddr = destAddr;
		pk.destPort = destPort;
		pk.payload = "";				
		sendPackets.put(pk.seqNum, pk);	
	}
	
	public void sendDataWithARQ(DatagramSocket ds,InetAddress routerAddr,short routerPort,boolean isClient) throws IOException { //router's IP & port
		ds.setSoTimeout(receiveTimeOut);//do ARQ when time out
		int endIndex=buildSendQueue(1);//1: send for first packet. next startIndex should be endIndex+1

		//for check all ACKs
		byte[] receiveBuf = new byte[Packet.Buf_Len];//1024 receive buffer
		DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
		int tries=0;
		
		if(isClient)
			System.out.println(dateFormat.format(new Date())+"Sending HTTP request to server ...");
		else
			System.out.println(dateFormat.format(new Date())+"Sending HTTP response to client ...");
				
		while(true){
			if(endIndex<0||sendQueue.size()==0) //all packet sent
				break;
			//send packets in queue
			for(Integer i:sendQueue){
				Packet pk = sendPackets.get(i);
				DatagramPacket dpSend = new DatagramPacket(pk.getBytes(),pk.bytesLength(), routerAddr, routerPort);
				ds.send(dpSend);				
			}
			//check all ACKs
			while(true){
				try{
				   ds.receive(dpReceive);
				   Packet pk = new Packet();
				   pk.setBytes(dpReceive.getData(),dpReceive.getLength()); //decode

				   //because of drop packet by router or out of order, we must have a way to exit loop
				   if(endIndex>=sendPackets.size()){//for not  terminated normally
					   if(pk.type!=1)//may be 4(all data received) or 2(type=4 has been droped by router, send data now).  so exit receive procedure now, so exit send again.
						   return; 
				   }
					   
				   dpReceive.setLength(receiveBuf.length);//for receive next packet
				   if(isACKPacket(pk)){//pk.type==1: ACK packet, remove it from queue
					   if(sendQueue.size()==0){ //all packet get ACK
						   endIndex=buildSendQueue(endIndex+1);//build next group of packet to be sent
						   break;
					   }
				   }
					   
			    } catch (InterruptedIOException e) { 
	            	if(isClient) 
	            		break;//server not receive command from client, send again£¨avoid "Server does not response..." message£©
			    	
			    	tries++;
			    	if(tries >= Max_Timeout_Try){ //other size may exit already
			    		//System.out.println("not response, ACK failed!");
			    		return;
			    	}
			    	break; //receive time out, re-sent NACK packet in queue
                }		
			}			
		}
	}
	
	public boolean receiveDataWithACK(DatagramSocket ds,boolean isClient) throws IOException { //receive and keep a packet then send its ACK
		//ds.setSoTimeout(receiveTimeOut);//not need here(receive data, not ACK)
		byte[] receiveBuf = new byte[Packet.Buf_Len];//1024 receive buffer		
		//when receive one packet then send one ACK		
        DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
        receivePackets.clear();
        totalPacketNeedReceive=-1;//compare with receivePackets.size(), so init less than zero
        int tries=0;
        
		if(isClient)
			System.out.println(dateFormat.format(new Date())+"Receiving HTTP response from server ...");
		else
			System.out.println(dateFormat.format(new Date())+"Receiving HTTP request from client ...");
        
        while(true){
            try {
                ds.receive(dpReceive);
                //receive a packet, then send ACK
                InetAddress routerAddr = dpReceive.getAddress();
                int routerPort = dpReceive.getPort();
                Packet recPK = new Packet();
                recPK.setBytes(dpReceive.getData(),dpReceive.getLength());
                if(recPK.type == 2) //data
                  receivePackets.put(recPK.seqNum, recPK);//keep packets received, same seqNum(re-send the same packet) will overwrite. 
                if(recPK.type == 3) //end ACK. because of out-of order, this End ACK packet may received before data packet, so need check total packet number has received
                	totalPacketNeedReceive = recPK.seqNum-1;
                Packet ackPK = new Packet();//here must create a new Packet ackPK, can not use recPK and changed its value to send(thid will change value in HashMap receivePackets)
                ackPK.clone(recPK);//keep some value unchanged(such as destAddr,destPort assigned by router)
                ackPK.type = 1;//ACK
                //ackPK.destAddr = destAddr;//actually pk.destAddr&pk.destPort already changed to right value by router now
                //ackPK.destPort = destPort;
                ackPK.payload = ""; //keep seqNum unchanged(send this seqNum's ACK)       
				DatagramPacket dpSend = new DatagramPacket(ackPK.getBytes(),ackPK.bytesLength(), routerAddr, routerPort);
				ds.send(dpSend);	
				if(totalPacketNeedReceive==receivePackets.size()){ //all data received
	                ackPK.type = 4;//all data received ACK
					dpSend = new DatagramPacket(ackPK.getBytes(),ackPK.bytesLength(), routerAddr, routerPort);
					ds.send(dpSend);//this packet may be drop by router						
                    break;
				}
            } catch (InterruptedIOException e) { //time out, may retry receive
            	if(!isClient) 
            		continue;//server£¬ ready to receive command from client
            	//client, no response, then  exit
		    	tries++;
		    	if(tries >= Max_Timeout_Try){ //other size may exit already
		    		System.out.println("Server does not response, try connect it again!");
		    		return false;
		    	}            	
            }
        }
        return true;
	}
	
	public String getPayload(){ //all Http Message
		String result = "";
		int len = receivePackets.size();
		for(int i=1; i<=len;i++){ //according seqNum order
			Packet pk = receivePackets.get(i);
			result = result+pk.payload;
		}
		return result;
	}
	public void loadConfig() throws IOException{
	  final String Config_File = "config.ini";
	  Properties prop = new Properties();
      try{	  
        FileInputStream in = new FileInputStream(Config_File);   
        prop.load(in);   
        in.close();   	
      } catch (IOException e) {  //file not exist
      }

  	  routerHostCfg = prop.getProperty("RouterHost","127.0.0.1"); //127.0.0.1: default value
  	  routerPortCfg = Integer.parseInt(prop.getProperty("RouterPort","3000")); //3000: default port
  	  clientHostCfg = prop.getProperty("ClientHost","127.0.0.1");
  	  clientPortCfg = Integer.parseInt(prop.getProperty("ClientPort","8083")); 
  	  serverHostCfg = prop.getProperty("ServerHost","127.0.0.1");
  	  serverPortCfg = Integer.parseInt(prop.getProperty("ServerPort","8082"));
  	  
  	  receiveTimeOut = Integer.parseInt(prop.getProperty("ReceiveTimeOut",Time_Out));//default 5 second
	}
}

/*
class Handshake { //establish a connection at start(for Multi-Requests Support)
    //receive Handshake Packet: type =1 (ACK) and seqNum=0. 
	//received data Packet will be discarded and no ACK response, so client may need re-send data packet
	InetAddress senderAddr;//get from Packet[5]-Packet[8],set it by router
	short senderPort;//get from Packet[9]-Packet[10],set it by router
	public DatagramSocket accept(int port) throws IOException{ 
		DatagramSocket socket = new DatagramSocket(port);//can not create multi-instance with same port
		byte[] receiveBuf = new byte[Packet.Buf_Len];//1024 receive buffer
		DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
		while(true){
            try {
            	socket.receive(dpReceive);
                //receive a packet, then send ACK
                InetAddress routerAddr = dpReceive.getAddress();
                int routerPort = dpReceive.getPort();
                Packet pk = new Packet();
                pk.setBytes(dpReceive.getData(),dpReceive.getLength());
                if(pk.type == 1&&pk.seqNum==0){ //handshake ACK(other type of packet will discarded)
                  //pk.destAddr and pk.destPort has been replaced by router using sender's addr & port,so not need set here
                  senderAddr = pk.destAddr;
                  senderPort = pk.destPort;
                  pk.payload = ""; //keep type and seqNum unchanged(hand shake ACK)
		  		  DatagramPacket dpSend = new DatagramPacket(pk.getBytes(),pk.bytesLength(), routerAddr, routerPort);
		  		  socket.send(dpSend);	
                  break;
                }
            } catch (InterruptedIOException e) {// time out not need here(timeout=0)
            }				
		}
		return socket;
	}
	public boolean handShaked(DatagramSocket ds,InetAddress routerAddr,short routerPort,InetAddress destAddr,short destPort,int timeOut) throws IOException{
		final int MAX_TRY = 5;
				
		boolean result = false; 
		byte[] receiveBuf = new byte[Packet.Buf_Len];//1024 receive buffer
		DatagramPacket dpReceive = new DatagramPacket(receiveBuf, receiveBuf.length);
		Packet pk = new Packet();
		//-----hand shake token---
		pk.type = 1;
		pk.seqNum = 0;
		//------------------------
		pk.destAddr = destAddr;
	    pk.destPort = destPort;
		pk.payload="";
		DatagramPacket dpSend = new DatagramPacket(pk.getBytes(),pk.bytesLength(), routerAddr, routerPort);
		ds.setSoTimeout(timeOut);
		
		System.out.println("Hand shaking ...");
		int tries = 0;
		while(true){
            ds.send(dpSend);							   
			try{
			   ds.receive(dpReceive);
			   Packet recPk = new Packet();
			   recPk.setBytes(dpReceive.getData(),dpReceive.getLength()); //decode
			   dpReceive.setLength(receiveBuf.length);//for receive next packet
			   if(pk.type == 1&&pk.seqNum==0){ //handshake ACK(other type of packet will discarded)
				   result = true;//hand shake ok
				   break;
			   }
		    } catch (InterruptedIOException e) { 
		    	if(tries>=MAX_TRY){
		    		System.out.println("Server not response, hand shake failed!");
		    		break;
		    	}
		    	tries += 1;
                System.out.println(String.format("Time out, try %d/%d times",tries, MAX_TRY)); 
		    }
		}			
		return result;
	}	
}
*/
