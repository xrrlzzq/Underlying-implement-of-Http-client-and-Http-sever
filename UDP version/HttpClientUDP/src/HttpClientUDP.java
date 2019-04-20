
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter; 

class HttpClientLib { 
	static int myReadIndex; //for pass value out of myReadLine();

	public static boolean sendRequestReceiveResponse(SelectARQ selARQ,DatagramSocket socket,InetAddress routerAddr,short routerPort) throws IOException{
        int tries = 0;//because router deliver the previous buffered ACK, so can not get data from server, try again
        while(true){
          selARQ.sendDataWithARQ(socket,routerAddr,routerPort,true); //send to router
          if(!selARQ.receiveDataWithACK(socket,true)){//receive data from server
        	  tries++; //server not response, try again
        	  if(tries>3)
        	    return false;
           }
          else
        	  break;
         }
        return true;
	}
	
	public static String SocketDoHead(String urlParam, String charset) throws IOException {  
        String result = "";  
          
        SelectARQ selARQ = new SelectARQ();
        selARQ.loadConfig();
        DatagramSocket socket = new DatagramSocket(selARQ.clientPortCfg);//receive data port
        
        try {  
            URL url = new URL(urlParam);  
            String host = url.getHost();  
            int port = url.getPort();  
            if (-1 == port) {  
                port = 80;  
            }  

            String urlQuery = url.getQuery(); //for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b
            String path = url.getPath();
            if(path.equals("")) //path always should start at "/"
            	path = "/";
            if(!(urlQuery==null)&&!urlQuery.equals(""))
            	path += "?"+urlQuery;  //GET pass QueryString at URI(for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b)
            
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("HEAD " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            //Content-Length:
            //HEAD or GET does not send body data, so do not set Content-Length value, otherwise http server will wait body data until time out and rest the connection
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait
                        
            InetAddress destAddr = InetAddress.getByName(host); //server IP
            short destPort = (short)port;//server port
            InetAddress routerAddr = InetAddress.getByName(selARQ.routerHostCfg); //router IP
            short routerPort = (short) selARQ.routerPortCfg ;//router port

            String payload = strBuf.toString();             
            selARQ.buildSendBuf(destAddr,destPort,payload);
            if(!sendRequestReceiveResponse(selARQ,socket,routerAddr,routerPort))
              return "";//server not response
            result = selARQ.getPayload();//get HTTP Head&Body
            
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            socket.close();  
        }  
        return result;  
    }  
    
    public static String SocketDoPost(String urlParam, String charset) throws IOException {  
        String result = "";  
        SelectARQ selARQ = new SelectARQ();
        selARQ.loadConfig();
        DatagramSocket socket = new DatagramSocket(selARQ.clientPortCfg);//receive data port
        try {  
            URL url = new URL(urlParam);  
            String host = url.getHost();  
            int port = url.getPort();  
            if (-1 == port) {  
                port = 80;  
            }  
            
            String urlQuery = url.getQuery();//for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b
            String path = url.getPath();         
            if(path.equals("")) //path always should start at "/"
            	path = "/";

            //NOTE:
            //POST contains parma data at body, do not add params to path,
            //Content-Length must be set to the length of params, if less than params's length,
            //only partial params sent to server, if great than param's length, server will wait until time out and reset the connecion
            // if(!(urlQuery==null)&&!urlQuery.equals("")) //POST also need this parms 
            // 	path += "?"+urlQuery;  //pass QueryString at URI(for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b)
            
            int ContentLength = 0;
            if(!(urlQuery==null)&&!urlQuery.equals(""))
            	ContentLength = urlQuery.length();
            	
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("POST " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            strBuf.append("Content-Length: "+ContentLength+"\r\n");//Content-Length must be assigned for POST method
            strBuf.append("\r\n");  // this space line means head ended,otherwise server will wait

            //POST pass params at this body ,Content-Length is the length of this area
            if(!(urlQuery==null)&&!urlQuery.equals("")) //for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b 
            	strBuf.append(urlQuery);

            InetAddress destAddr = InetAddress.getByName(host); //server IP
            short destPort = (short)port;//server port
            InetAddress routerAddr = InetAddress.getByName(selARQ.routerHostCfg); //router IP
            short routerPort = (short) selARQ.routerPortCfg ;//router port

            String payload = strBuf.toString(); 
            selARQ.buildSendBuf(destAddr,destPort,payload);
            if(!sendRequestReceiveResponse(selARQ,socket,routerAddr,routerPort))
                return "";//server not response
            result = selARQ.getPayload();//get HTTP Head&Body
            
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            socket.close();  
        }  
        return result;  
    }  
  
    public static String SocketDoGet(String urlParam, String charset,String cmd) throws IOException {  
        String result = "";  
        SelectARQ selARQ = new SelectARQ();
        selARQ.loadConfig();
        DatagramSocket socket = new DatagramSocket(selARQ.clientPortCfg);//receive data port
        try {  
            URL url = new URL(urlParam);  
            String host = url.getHost();  
            int port = url.getPort();  
            if (-1 == port) {  
                port = 80;  
            }  
            
            String urlQuery = url.getQuery();//for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b
            String path = url.getPath();
            if(path.equals("")) //path always should start at "/"
            	path = "/";
            if(!(urlQuery==null)&&!urlQuery.equals(""))
            	path += "?"+urlQuery;  //GET pass QueryString at URI(for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b)
            
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("GET " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            //Content-Length:
            //HEAD or GET does not send body data, so do not set Content-Length value, otherwise http server will wait body data until time out and rest the connection
            //strBuf.append("Content-Length: ").append(strBuf.toString().getBytes().length).append("\r\n");  
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait  
            
            if(cmd.equals("GETQUERY")){
      	        result = urlQuery; // http://127.0.0.1:8080?x=a&y=b £¬return x=a&y=b
      	        return result;
            }            
            if(cmd.equals("GETREQUEST")){
            	result = strBuf.toString();
                return result;
            }
            
            InetAddress destAddr = InetAddress.getByName(host); //server IP
            short destPort = (short)port;//server port
            InetAddress routerAddr = InetAddress.getByName(selARQ.routerHostCfg); //router IP
            short routerPort = (short) selARQ.routerPortCfg ;//router port

            String payload = strBuf.toString(); 
            selARQ.buildSendBuf(destAddr,destPort,payload);
            if(!sendRequestReceiveResponse(selARQ,socket,routerAddr,routerPort))
                return "";//server not response
            result = selARQ.getPayload();//get HTTP Head&Body
                              
            String line = null;  
            String line1 = "";
            String firstLine = "";
            myReadIndex=0;
            
            int contentLength = 0;  
            do {  
                line = myReadLine(result, 0, charset); //read data from server
                
                if(firstLine.equals(""))
                	firstLine = line;                
                line1 +=line;//line1 contains head info
                
                if (line.startsWith("Content-Length")) {  
                    contentLength = Integer.parseInt(line.split(":")[1].trim());  
                }  
            } while (!line.equals("\r\n"));// if meets a space line ,means request head ended.
            
            switch(cmd){
            case "GETRAW": //all data return
            	  result = line1 + myReadLine(result, contentLength, charset); 
            	  break;
            case "GETHEAD"://Head return
            	  result = line1;
            	  break;
            case "GETBODY"://Body return
            	  result = myReadLine(result, contentLength, charset);
            	  break;
            case "GETSTATUS": //first line
          	      result = firstLine;
          	      break;
            case "GETCODE":
            	  String[] sen = firstLine.split("\\ ");
            	  result = sen[1];
            	  break;
            default:
                  result = "Bad Command, all Command is: Post Head Redirect GetRequest GetRaw GetHead GetBody GetQuery GetStatus GetCode ";
                  break;
            }
            
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            socket.close();  
        }  
        return result;              
    }  
  

    public static String SocketDoRedirect(String urlParam, String charset) throws IOException {  
        String result = "";  
        String urlRedirect="";
        String headInfo ="";

        SelectARQ selARQ = new SelectARQ();
        selARQ.loadConfig();
        DatagramSocket socket = new DatagramSocket(selARQ.clientPortCfg);//receive data port
        try {  
            URL url = new URL(urlParam);  
            String host = url.getHost();  
            int port = url.getPort();  
            if (-1 == port) {  
                port = 80;  
            }  
            
            String urlQuery = url.getQuery();//for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b
            String path = url.getPath();
            if(path.equals("")) //path always should start at "/"
            	path = "/";
            if(!(urlQuery==null)&&!urlQuery.equals(""))
            	path += "?"+urlQuery;  //GET pass QueryString at URI(for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b)            
            
            //send HEAD request to check whether this a redirection
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("HEAD " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            //Content-Length:
            //HEAD or GET does not send body data, so do not set Content-Length value, otherwise http server will wait body data until time out and rest the connection
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait
                        
            InetAddress destAddr = InetAddress.getByName(host); //server IP
            short destPort = (short)port;//server port
            InetAddress routerAddr = InetAddress.getByName(selARQ.routerHostCfg); //router IP
            short routerPort = (short) selARQ.routerPortCfg ;//router port

            String payload = strBuf.toString(); 
            selARQ.buildSendBuf(destAddr,destPort,payload);
            if(!sendRequestReceiveResponse(selARQ,socket,routerAddr,routerPort))
                return "";//server not response
            result = selARQ.getPayload();//get HTTP Head&Body

            String line = null;  
            myReadIndex=0;
            do {  
                line = myReadLine(result, 0, charset); //read data from server
                headInfo +=line;          
                if(line.contains("Location:")){
                   urlRedirect = line.substring(9).trim();//9= length(Location:) //trim(): remove \n\r and space
                }
            } while (!line.equals("\r\n")); // if meets a space line ,means request head ended.  

            //--------------if redirection occured, head returned info like this ----------
            //page1.asp redirect to page2.asp
            //page1.asp:
            //<%
            //Response.Redirect "http://127.0.0.1:8080/page2.asp"
            //%>
            //Head info return from Page1.asp:
            
            //HTTP/1.1 302 Object moved
            //Cache-Control: private
            //Content-Length: 129
            //Content-Type: text/html
            //Location: http://127.0.0.1:8080/page2.asp
            //Server: Microsoft-IIS/10.0
            //Set-Cookie: ASPSESSIONIDQADSTDAB=HHKFBKFANOMIPAMEEEKNIOKE; path=/
            //Date: Sat, 07 Oct 2017 11:59:45 GMT
            //Connection: keep-alive
            //-----------------------------------------------------------------------------
            if(!headInfo.contains(" 302 ")){
            	return headInfo.toString()+"\r\nRedirection does not occured at server side";
            }
            if(urlRedirect.equals("")){
            	return headInfo.toString()+"\r\nRedirection occured,but new URL is empty";
            }
            	        
            //these is a redirection, send GET to new URL
            result = SocketDoGet(urlRedirect, "utf-8","GETRAW");
            
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            socket.close();  
        }  
        return result;              
    } 

  private static String myReadLine(String input, int contentLength, String charset) throws IOException {  
    List<Byte> lineByte = new ArrayList<Byte>();
    byte[] inStream = input.getBytes();//like inputStream
    byte tempByte;  
    int cumsum = 0;  
    if (contentLength != 0) {  
        do {  
            tempByte = (byte) inStream[myReadIndex];
            myReadIndex++;
            lineByte.add(Byte.valueOf(tempByte));  
            cumsum++;  
        } while (cumsum < contentLength);  
    } else {  
        do {  
            tempByte = (byte) inStream[myReadIndex];
            myReadIndex++;
            lineByte.add(Byte.valueOf(tempByte));  
        } while (tempByte != 10);// 10: \n
    }  

    byte[] resutlBytes = new byte[lineByte.size()];  
    for (int i = 0; i < lineByte.size(); i++) {  
        resutlBytes[i] = (lineByte.get(i)).byteValue();  
    }  
    return new String(resutlBytes, charset);  
 }  

}
public class HttpClientUDP {

	public static void main(String[] args) throws IOException {
		HttpClientLib HttpClient = new HttpClientLib();
		String cmd,url;
		if(args.length <= 1){
	      System.out.println("usage: java HttpClientUDP Command URL");
	      System.out.println("Command: Post Head GetRequest GetRaw GetHead GetBody GetQuery GetStatus GetCode");
	      System.out.println("Redirection(URL1 sould redirect to URL2 at server side): java HttpClientUDP Redirect URL1");
	      return;
	    }
	    else { //CMD: filename.class is sensitive
	      cmd = args[0].toUpperCase(); 
	      url = args[1];	    		    	
	    }	    
		
		String msg="";
		if(cmd.equals("HEAD")){
		    msg = HttpClient.SocketDoHead(url, "utf-8");
		}		
		else if(cmd.equals("POST")){		
			msg = HttpClient.SocketDoPost(url, "utf-8");
		}
		else if(cmd.equals("REDIRECT")){
			msg = HttpClient.SocketDoRedirect(url, "utf-8");
		}
		else{
		    msg = HttpClient.SocketDoGet(url, "utf-8",cmd);
		}
		
		
		System.out.println(msg);

	}

}
