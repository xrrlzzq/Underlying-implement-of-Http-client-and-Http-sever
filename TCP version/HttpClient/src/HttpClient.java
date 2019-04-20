import java.net.Socket;
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
	public static String SocketDoHead(String urlParam, String charset) {  
        String result = "";  
          
        Socket socket = null;  
        OutputStreamWriter outSW = null;  
        InputStream inStream = null;  
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
            
            socket = new Socket(host, port);  
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("HEAD " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
           // strBuf.append("Content-Length: ").append(strBuf.toString().getBytes().length).append("\r\n");  //GET *.asp will rest connection
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait
                        
            outSW = new OutputStreamWriter(socket.getOutputStream()); //at client side,OutputStream is data send to server
            outSW.write(strBuf.toString());  
            outSW.flush();  //send request to server
            
            inStream = socket.getInputStream(); //at client side,inputStream is data get from server  
            String line = null;  
            do {  
                line = myReadLine(inStream, 0, charset); //read data from server
                result +=line;                
            } while (!line.equals("\r\n")); // if meets a space line ,means request head ended.  
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            if (outSW != null) {  
                try {  
                    outSW.close();  
                } catch (IOException e) {  
                    outSW = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
            if (inStream != null) {  
                try {  
                    inStream.close();  
                } catch (IOException e) {  
                    inStream = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
        }  
        return result;  
    }  
    
    public static String SocketDoPost(String urlParam, String charset) {  
        String result = "";  
        Socket socket = null;  
        OutputStreamWriter outSW = null;  
        InputStream inStream = null;  
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
            if(!(urlQuery==null)&&!urlQuery.equals("")) //POST also need this parms 
            	path += "?"+urlQuery;  //pass QueryString at URI(for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b)
            
            socket = new Socket(host, port);  
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("POST " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            strBuf.append("Content-Length: 0\r\n"); //POST has no body, so length is 0
            strBuf.append("\r\n");  // this space line means head ended,otherwise server will wait

            //POST pass QueryString at FORM 
            if(!(urlQuery==null)&&!urlQuery.equals("")) //for exam, http://127.0.0.1:8080?x=a&y=b, urlQuery is x=a&y=b 
            	strBuf.append(urlQuery);

            outSW = new OutputStreamWriter(socket.getOutputStream());//at client side,OutputStream is data send to server  
            outSW.write(strBuf.toString());  
            outSW.flush();  //send request to server
            inStream = socket.getInputStream(); //at client side,inputStream is data get from server 
            String line = null;  
            int contentLength = 0;  
            do {  
                line = myReadLine(inStream, 0, charset); //read data from server 
                result +=line;
                
                if (line.startsWith("Content-Length")) {  
                    contentLength = Integer.parseInt(line.split(":")[1].trim());  
                }  
            } while (!line.equals("\r\n")); // if meets a space line ,means request head ended.    
            result += myReadLine(inStream, contentLength, charset); //normorally Post does not return body ,so use result += body 
        } catch (Exception e) {  
            throw new RuntimeException(e);  
        } finally {  
            if (outSW != null) {  
                try {  
                    outSW.close();  
                } catch (IOException e) {  
                    outSW = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
            if (inStream != null) {  
                try {  
                    inStream.close();  
                } catch (IOException e) {  
                    inStream = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
        }  
        return result;  
    }  
  
    public static String SocketDoGet(String urlParam, String charset,String cmd) {  
        String result = "";  
        Socket socket = null;  
        OutputStreamWriter outSW = null;  
        InputStream inStream = null;  
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
            
            socket = new Socket(host, port);  
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("GET " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            //strBuf.append("Content-Length: ").append(strBuf.toString().getBytes().length).append("\r\n"); //GET *.asp will rest connection 
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait  
            
            if(cmd.equals("GETQUERY")){
      	        result = urlQuery; // http://127.0.0.1:8080?x=a&y=b £¬return x=a&y=b
      	        return result;
            }            
            if(cmd.equals("GETREQUEST")){
            	result = strBuf.toString();
                return result;
            }
            
            outSW = new OutputStreamWriter(socket.getOutputStream()); //at client side,OutputStream is data send to server
            outSW.write(strBuf.toString());  
            outSW.flush();//send request to server
            
            inStream = socket.getInputStream(); //at client side,inputStream is data get from server 
            String line = null;  
            String line1 = "";
            String firstLine = "";
            
            int contentLength = 0;  
            do {  
                line = myReadLine(inStream, 0, charset); //read data from server
                
                if(firstLine.equals(""))
                	firstLine = line;                
                line1 +=line;//line1 contains head info
                
                if (line.startsWith("Content-Length")) {  
                    contentLength = Integer.parseInt(line.split(":")[1].trim());  
                }  
            } while (!line.equals("\r\n"));// if meets a space line ,means request head ended.
            
            switch(cmd){
            case "GETRAW": //all data return
            	  result = line1 + myReadLine(inStream, contentLength, charset); 
            	  break;
            case "GETHEAD"://Head return
            	  result = line1;
            	  break;
            case "GETBODY"://Body return
            	  result = myReadLine(inStream, contentLength, charset);
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
            if (outSW != null) {  
                try {  
                    outSW.close();  
                } catch (IOException e) {  
                    outSW = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
            if (inStream != null) {  
                try {  
                    inStream.close();  
                } catch (IOException e) {  
                    inStream = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
        }  
        return result;  
    }  
  

    public static String SocketDoRedirect(String urlParam, String charset) {  
        String result = "";  
        String urlRedirect="";
        String headInfo ="";

        Socket socket = null;  
        OutputStreamWriter outSW = null;  
        InputStream inStream = null;  
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
            socket = new Socket(host, port);  
            StringBuffer strBuf = new StringBuffer();  
            strBuf.append("HEAD " + path + " HTTP/1.1\r\n");  
            strBuf.append("Host: " + host+":"+ port + "\r\n");  
            strBuf.append("Connection: Keep-Alive\r\n");  
            strBuf.append("Content-Type: application/x-www-form-urlencoded; charset=utf-8 \r\n");  
            //strBuf.append("Content-Length: ").append(strBuf.toString().getBytes().length).append("\r\n");  //GET *.asp will rest connection
            strBuf.append("\r\n");// this space line means head ended,otherwise server will wait
                        
            outSW = new OutputStreamWriter(socket.getOutputStream()); //at client side,OutputStream is data send to server
            outSW.write(strBuf.toString());  
            outSW.flush();  //send request to server
            
            inStream = socket.getInputStream(); //at client side,inputStream is data get from server  
            String line = null;  
            do {  
                line = myReadLine(inStream, 0, charset); //read data from server
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
            if (outSW != null) {  
                try {  
                    outSW.close();  
                } catch (IOException e) {  
                    outSW = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
            if (inStream != null) {  
                try {  
                    inStream.close();  
                } catch (IOException e) {  
                    inStream = null;  
                    throw new RuntimeException(e);  
                } finally {  
                    if (socket != null) {  
                        try {  
                            socket.close();  
                        } catch (IOException e) {  
                            socket = null;  
                            throw new RuntimeException(e);  
                        }  
                    }  
                }  
            }  
        }  
        return result;  
    }
    
    private static String myReadLine(InputStream inStream, int contentLength, String charset) throws IOException {  
        List<Byte> lineByte = new ArrayList<Byte>();  
        byte tempByte;  
        int cumsum = 0;  
        if (contentLength != 0) {  
            do {  
                tempByte = (byte) inStream.read();  
                lineByte.add(Byte.valueOf(tempByte));  
                cumsum++;  
            } while (cumsum < contentLength);  
        } else {  
            do {  
                tempByte = (byte) inStream.read();  
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


public class HttpClient {

	public static void main(String[] args) {
		HttpClientLib HttpClient = new HttpClientLib();
		String cmd,url;
		if(args.length <= 1){
	      System.out.println("usage: java HttpClient Command URL");
	      System.out.println("Command: Post Head GetRequest GetRaw GetHead GetBody GetQuery GetStatus GetCode");
	      System.out.println("Redirection(URL1 sould redirect to URL2 at server side): java HttpClient Redirect URL1");
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
