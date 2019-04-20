import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
class Request   
{   
	private Map param;
	public int contentlength=0;   
	public String method=null;
	public String action=null;
	public String path=null;
	public String query=null;
	public String postbody=null;
	public String clientrequest=null;
	public String contenttype=null;
	public boolean urlencoded;
	public boolean stdown;
	public boolean browseagent;
	public String newline=null;
	public String lessthan=null;
	public String greatthan=null;
	public String space=null;
	
	public void CreateParamMap() throws UnsupportedEncodingException
	{
		if(query.equals("")&&postbody.equals(""))
			return;
		param=new HashMap();
		String[] part1=query.split("\\&");
		int i=0;
		for(i=0;i<part1.length;i++)
		{
			String[] part2=part1[i].split("\\=");
			if(part2.length==2)
			{
				String key=URLDecoder.decode(part2[0], "utf-8");
				String value=URLDecoder.decode(part2[1], "utf-8");
				param.put(key, value);
			}
		}
		String[] postPart1=postbody.split("\\&");
		
		for(i=0;i<postPart1.length;i++)
		{
			String[] postPart2=postPart1[i].split("\\=");
			if(postPart2.length==2)
			{
				String key=URLDecoder.decode(postPart2[0], "utf-8");
				String value=URLDecoder.decode(postPart2[1], "utf-8");
				param.put(key, value);
			}
		}
	}
	
	public void GetFieldFromRequest(DatagramSocket socket,SelectARQ selARQ) throws IOException
	{   path="";
		contentlength=0;
		method="";
		action="";
		query="";
		postbody="";
		clientrequest="";
		urlencoded=false;
		contenttype="";
		stdown=false;
		
        selARQ.receiveDataWithACK(socket,false);//receive data from server        
        String httpHeadBody = selARQ.getPayload();//get HTTP Head&Body
        if(httpHeadBody.equals(""))
        	return;
        
        int pos = httpHeadBody.indexOf("\r\n\r\n");
		String httphead=httpHeadBody.substring(0, pos);
		String httpbody = httpHeadBody.substring(pos+4, httpHeadBody.length());

		String[] httprequest=httphead.split("\\\r\n");
		String line=null;
		String firstline="";
		int i=0;
		for(i=0;i<httprequest.length;i++)
		{
			line=httprequest[i];
			if(firstline.equals(""))
			{
				firstline=line;
				String[] temp=firstline.split("\\ ");
				method=temp[0];
				path=temp[1];
				
			}
			clientrequest+=line+"\r\n";
			if(line.startsWith("Content-Length"))
				contentlength=Integer.parseInt(line.split(":")[1].trim());
			
			if(line.startsWith("Content-Type"))
			{
				contenttype=line.split(":")[1].trim();
				urlencoded=(contenttype.indexOf("urlencoded")!=-1);
				
			}
		}
		if(method.equals("POST")&&contentlength!=0)
		{
			postbody=httpbody;
			clientrequest+="\r\n"+postbody;
		}
	
		String[] pathdata;
		pathdata=path.split("\\?");
		if(pathdata.length==2)
			query=pathdata[1];
		CreateParamMap();
		browseagent = clientrequest.indexOf("User-Agent: Mozilla") !=-1;//for display correctly. true if contains: User-Agent: Mozilla
        newline = browseagent?"<br>\r\n":"\r\n";//for browser, <br>, for console£¬ "\r\n"
    	lessthan = browseagent?"&lt":"<" ;// < token
    	greatthan =browseagent?"&gt":">" ;// > token
    	space = browseagent?"&nbsp":" ";//for Browser &nbsp, console " "
		System.out.println(clientrequest);
	}
	public String getparam(String key)
	{
		String result="";
		if(param!=null)
			result=(String)param.get(key);
		return result;
	}

}

class Respond
{    
	 
	 public static final String ROOT_DIR = System.getProperty("user.dir")+File.separator+"ROOT"; 
	 DatagramSocket socket;
	 Request request;
	 SelectARQ selARQ; 
	 InetAddress routerAddr;
	 short routerPort;
	 InetAddress destAddr;
	 short destPort;
     String payload;
	 
	 public void bindRequest(DatagramSocket socket ,Request request)
	 {
		 this.socket=socket;
		 this.request=request;
	 }
	 public void doRespond(InetAddress clientAddr,short clientPort,SelectARQ ARQ) throws IOException
	 {
		 String filedir=ROOT_DIR;
		 String respondbody;
		 selARQ = ARQ;
         routerAddr = InetAddress.getByName(selARQ.routerHostCfg); //router IP
         routerPort = (short) selARQ.routerPortCfg ;//router port
    	 destAddr = clientAddr;
    	 destPort = clientPort;
				          
		 File file=new File(filedir);
		 if(!file.exists())
		 {
			 respondbody="Sever dictionary "+filedir+" does not exist";
			 String httprespond="HTTP/1.1 404 Not Found\r\n"+
			                    "Content-Length:"+respondbody.length()+"\r\n"+
					            "Content-Type: text/html; charset=utf-8\r\n"+
			                    "Server: xrserver\r\n"+
					            "\r\n"+
			                    respondbody;
			 try
			 {
		         payload = httprespond; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
			 }catch(Exception e)
			 {
				 e.printStackTrace();
			 }
			 return;
		 }
		 String action=request.getparam("action");
		if(action==null)
			action="cmd_error";
		 switch(action.toUpperCase())
		 {
		 case "DIR":
			       doDir(request,this,file);
			       break;
		 case "WRITE":
			       doWrite(this,request);
			       break;
		 case "READ":
			       doRead(this,request);
			       break;
		 case "DOWNLOAD":
		 		   doDownload(this,request);
		           break;
		 default:
			   doGuide(this,request);
			   break;
		 }
		 
		 
	 }
	 public List<String> GetAllDictionary(File filepath, List<String> pathname)
	 {
		 File[] files=filepath.listFiles();
		 if(files==null)
			 return pathname;
		 for(File f:files)
		 {
			 if(f.isDirectory())
			 {
				 pathname.add("\"type\": \"DIRECTORY\";\"pathName\": \""+f.getPath()+"\";\"length\": "+f.length()+";\"modificationTime\": "+f.lastModified());
				 GetAllDictionary(f,pathname);
			 }

			 else
				 pathname.add("\"type\": \"FILE\";\"pathName\": \""+f.getPath()+"\";\"length\": "+f.length()+";\"modificationTime\": "+f.lastModified());	 
		 }
		 return pathname;
	 }
	 public void doDir(Request request,Respond respond,File file)
	 {
		 String respondmesg;
		 List<String> pathname=new ArrayList<String>();
		 pathname=GetAllDictionary(file,pathname);
		 if(pathname==null)
			 respondmesg="found nothing";
		 else
		 {
			 String format=request.getparam("format");
			 if(format==null)
			 {
				 format="JOSN";
			 }
			 respondmesg=DealFormat(pathname,format.toUpperCase(),request);
		 }
		 String httpinfo="HTTP/1.1 200 OK\r\n"+
					"Content-Type: text/html; charset=utf-8\r\n" +  
	                "Content-Length: "+respondmesg.length()+"\r\n" +  
	                "\r\n" +
	                respondmesg;
			try
			{
		         payload = httpinfo; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
			}catch(Exception e)
			{
				e.printStackTrace();
			}
	 }
	 public String DealFormat(List<String> pathname , String format,Request request)
	 {
		 String result="";
		 String mainline;
		 int i=0;
		 for(i=0;i<pathname.size();i++)
		 {
			 String name =pathname.get(i);
			 mainline=MainLine(request,name,format);
			 switch(format){
	            case "HTML": 
	            	  result = result + mainline+request.newline;
	            	  break;
	            case "TEXT":
	            	  result = result + mainline+request.newline;
	            	  break;
	            case "XML": //IE can explore XML, so use "\r\n" directly(newline not correct for browser to display XML)
	            	  result = result +mainline;
	            	  if (i == pathname.size()-1) { //last one
	            		  result = "<FileList>\r\n"+result+"</FileList>\r\n";
	            		  result = "<?xml version=\"1.0\"?>\r\n"+
	            				    result;
	            		  if(request.browseagent){ //for browse to display XML correctly            			  
	            		    result = "<textarea rows=50 cols=200>\r\n"+result+"</textarea>";
	            		  }
	            	  }
	          	      break;            	  
	            default: //default use JSON format
	            case "JSON":
	          	  if (i != pathname.size()-1) { //not last one
	        	          result =result +
	        	                  request.space+request.space+request.space+request.space+"{"+request.newline+ 
	          	    		  mainline+
	          	    		  request.space+request.space+request.space+request.space+"},"+request.newline;
	        	       }
	          	  else{ //last one
	        	          result =result +
	        	                  request.space+request.space+request.space+request.space+"{"+request.newline+ 
	          	    		  mainline+
	          	    		  request.space+request.space+request.space+request.space+"}"+request.newline;
	          		  result ="{"+request.newline+
	          				   request.space+"\"FileStatuses\":"+request.newline+
	          				   "{"+request.newline+ 
	          				   request.space+request.space+"\"FileStatus\":"+request.newline+
	          				   request.space+request.space+"["+request.newline+
	          				       result +
	          				   request.space+request.space+"]"+request.newline+
	          				   request.space+"}"+request.newline+
	          				   "}"+request.newline;  
	          	  }
	          	  break;
	            }		    
			}
	    	return result;
		
		 
	 }
	 public String MainLine(Request request , String name , String format )
	 {
		 String result="";
		 String[] info=name.split("\\;");
		 boolean lastline;
		 int i=0;
		 for(i=0;i<info.length;i++)
		 {
			 lastline=(i==info.length-1);
			 switch(format){
	            case "HTML": 
	            	  result = result + "<p>"+info[i]+request.space;
	              	  if(lastline)
	              		  result = result+request.newline;
	            	  break;
	            case "XML": //"type":"FILE" => <type>FILE</type>
	            	  String tmp[] = info[i].split(": "); ////": " make a distinguish with filepath (g:\)  (:+space)
	            	  String token = tmp[0].replaceAll("\"", "");
	            	  String value = tmp[1].replaceAll("\"", "");
	            	  result = result +"     <"+token+">"+value+"</"+token+">\r\n";
	              	  if(lastline)
	              		  result = "   <File>\r\n"+result+"   </File>\r\n";
	          	      break;            	                	
	              case "TEXT":
	            	  result = result + info[i]+request.space+request.space+request.space;
	            	  if(lastline)
	            		  result = result+request.newline;
	            	  break;
	            default: //default is JSON(there is no break)
	            case "JSON":
	          	  if(!lastline)
	          	      result = result +request.space+request.space+request.space+request.space+request.space+ info[i]+","+request.newline;
	          	  else
	          		  result = result +request.space+request.space+request.space+request.space+request.space+ info[i]+request.newline;
	          	  break;
		 }
		 } 
	  return result;
	 }
	 public void doWrite(Respond respond ,Request request) throws IOException
	 {
		 String respondmesg="";
			String httpfirstline="HTTP/1.1 200 OK\r\n";
			String filename=request.getparam("file");
			String text=request.getparam("text");
			String textoverwrite=request.getparam("overwrite");
			boolean overwrite=textoverwrite!=null&&textoverwrite.equalsIgnoreCase("on");
			if(filename==null||text==null)
				respondmesg="Please assign the file name and text to write this way: "
			  			   +request.newline+"http://localhost:port?action=WRITE&file=FILENAME&overwrite=SWITCH&text=STRING, "
			  	 		   +request.newline+"FILENAME can contains path information,SWITCH=on|off,text=content to write to the file."		  	 		   
		                   +request.newline+"GET verb(text=STRING): used for WRITE only, STRING will be written to FILENAME"
	 	                   +request.newline+"POST verb("+request.lessthan+"textarea rows=20 cols=30 name=\"text\""+request.greatthan+"STRING"+request.lessthan+"/textarea"+request.greatthan+"): used for HTTP POST verb, "
	                       +request.newline+"use "+request.lessthan+"FORM method=\"POST\""+request.greatthan+" to submit, STRING will be written to FILENAME";
			else
			{   String fullname;
				if(filename.contains(":"))
					fullname=filename;
				else
					fullname=ROOT_DIR+File.separator+filename;
				if(fullname.toUpperCase().indexOf(ROOT_DIR.toUpperCase())==-1)
					{
					respondmesg = "Access denied: file "+fullname+" is out of the working directory "+ROOT_DIR;
                    httpfirstline="HTTP/1.1 401 Acess Denied\r\n";
					}
				if(respondmesg.equals(""))
				{
					File file=new File(fullname);
					respondmesg=WriteFile(request,file,text,overwrite);
				}
			}
			String httpinfo=httpfirstline+
					"Content-Type: text/html; charset=utf-8\r\n" +  
	                "Content-Length: "+respondmesg.length()+"\r\n" +  
	                "\r\n" +
	                respondmesg;
			try
			{
		         payload = httpinfo; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		
		
		
	 }
	 public String WriteFile(Request request , File file , String text , boolean overwrite) throws IOException
	 {
		 
		 BufferedWriter bw=new BufferedWriter(new FileWriter(file,!overwrite));
		 bw.write(text);
		 bw.flush();
		 bw.close();
		 String result = "Following content successfully be written(by "+(overwrite?"overwrite":"append")+") to: "+file.getAbsolutePath()+request.newline;
			if (request.browseagent)
				result += "<textarea rows=50 cols=200>\r\n"+text+"</textarea>"; //for display any text file content in browser(include xml ,HTML source code)
			else
				result += text;
	    	
	    	return result; 
	 }
	 public String ReadFile(Request request,File file) throws IOException
	 {
		 String result="";
		 BufferedReader reader=new BufferedReader(new FileReader(file));
		 String line=null;
		 while((line=reader.readLine())!=null)
		 {
			 result= result+line.toString()+"\r\n";
		 }
		 reader.close();
		 if(request.browseagent)
		 {
			 result = "<textarea rows=50 cols=200>\r\n"+result+"</textarea>"; 
		 }
		 return result;
	 }
	 public void doRead(Respond respond , Request request) throws IOException
	 {
		 String respondmesg="";
			String httpfirstline="HTTP/1.1 200 OK\r\n";
			String filename=request.getparam("file");
			if(filename==null)
				respondmesg="please write file name in this way:http://localhost:port?action=READ&file=FILENAME, FILENAME can contains path information."; 
			else
			{   String fullname;
				if(filename.contains(":"))
					fullname=filename;
				else
					fullname=ROOT_DIR+File.separator+filename;
				if(fullname.toUpperCase().indexOf(ROOT_DIR.toUpperCase())==-1)
				{
				respondmesg = "Access denied: file "+fullname+" is out of the working directory "+ROOT_DIR;
                httpfirstline="HTTP/1.1 401 Acess Denied\r\n";
				}
				if(respondmesg.equals(""))
				{
					File file=new File(fullname);
					if(!file.exists())
					{
						respondmesg="the file is not exist";
						httpfirstline="HTTP/1.1 404 Not Found\r\n";
					}
					else 
						respondmesg=ReadFile(request,file);
				}
			}
			String httpinfo=httpfirstline+
					"Content-Type: text/html; charset=utf-8\r\n" +  
	                "Content-Length: "+respondmesg.length()+"\r\n" +  
	                "\r\n" +
	                respondmesg;
			try
			{
		         payload = httpinfo; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
			}catch(Exception e)
			{
				e.printStackTrace();
			}
	 }
	 public void doDownload( Respond respond,Request request ) throws IOException
	 {
		 String respondmesg="";
			String httpfirstline="HTTP/1.1 200 OK\r\n";
			String filename=request.getparam("file");
			if(filename==null)
				respondmesg="please assign file name in this way: http://localhost:port?action=DOWNLOAD&file=FILENAME, FILENAME can contains path information."; 
			else
			{   String fullname;
				if(filename.contains(":"))
					fullname=filename;
				else
					fullname=ROOT_DIR+File.separator+filename;
				if(fullname.toUpperCase().indexOf(ROOT_DIR.toUpperCase())==-1)
				{
				respondmesg = "Access denied: file "+fullname+" is out of the working directory "+ROOT_DIR;
                httpfirstline="HTTP/1.1 401 Acess Denied\r\n";
				}
				if(respondmesg.equals(""))
				{
					File file=new File(fullname);
					if(!file.exists())
					{
						respondmesg="the file is not exist";
						httpfirstline="HTTP/1.1 404 Not Found\r\n";
					}
					else{ 
						Download(request,respond,file);
						return;
					}
				}
			}
			String httpinfo=httpfirstline+
					"Content-Type: text/html; charset=utf-8\r\n" +  
	                "Content-Length: "+respondmesg.length()+"\r\n" +  
	                "\r\n" +
	                respondmesg;
			try
			{
		         payload = httpinfo; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
			}catch(Exception e)
			{
				e.printStackTrace();
			}
	 }
	 public void Download(Request request ,Respond respond,File file) throws IOException
	 {
		 String filename=file.getName();
		 String httphead="HTTP/1.1 200 OK\r\n"+
				         "Content-Type: application/octet-stream\r\n" +
				         "Content-Length:"+file.length()+"\r\n"+
				         "Content-Disposition: attachment; filename=\""+filename+"\"\r\n"+
				         "\r\n";
		 try
		 {
			 payload = httphead;
			 int num=0;
			 byte[] by=new byte[2048];
			 StringBuffer RequestBuffer = new StringBuffer(2048);
			 FileInputStream finput=new FileInputStream(file);
			 while(true)
			 {   try{
				   num=finput.read(by);
			     }catch(Exception e)
			     {
				    e.printStackTrace();
				    num=-1;
			     }
			     if(num==-1)
				    break;

			    for(int j = 0;j<num;j++){  
	           	   RequestBuffer.append((char)(by[j])); 
			    }
			    payload = payload+RequestBuffer.toString();
			    RequestBuffer.setLength(0);//empty
			 }
			 finput.close();
		 }catch(Exception e)
		 {
			e.printStackTrace(); 
		 }
				 
         selARQ.buildSendBuf(destAddr,destPort,payload);
         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router		 
	 }
	 public void doGuide(Respond respond ,Request request)
	 {
		 String respMsg =
		           "usage:"
                    +request.newline+"http://localhost:port?action=CMD&format=FORMAT&file=FILENAME&overwrite=SWITCH&text=STRING"    	
                    +request.newline+"CMD     : action to operate, include DIR,READ,WRITE,DOWNLOAD"
                    +request.newline+"FORMAT  : data format response to client, include JSON,XML,TEXT,HTML"
                    +request.newline+"FILENAME: filename to operate"
                    +request.newline+"SWITCH  : used for WRITE only, include overwrite=on|off"
                    +request.newline+"GET verb(text=STRING): used for WRITE only, STRING will be written to FILENAME"
                    +request.newline+"POST verb("+request.lessthan+"textarea rows=20 cols=30 name=\"text\""+request.greatthan+"STRING"+request.lessthan+"/textarea"+request.greatthan+"): used for HTTP POST verb, "
                  +request.newline+"use "+request.lessthan+"FORM method=\"POST\""+request.greatthan+" to submit, STRING will be written to FILENAME";
                  
  
  String respHTTP = 
		    "HTTP/1.1 200 OK\r\n" +  
          "Content-Type: text/html; charset=utf-8\r\n" +  
          "Content-Length: "+respMsg.length()+"\r\n" +  
          "\r\n" +
          respMsg;
  
          try {  
		         payload = respHTTP; 
		         selARQ.buildSendBuf(destAddr,destPort,payload);
		         selARQ.sendDataWithARQ(socket,routerAddr,routerPort,false); //send to router
           } catch (IOException e) {  
              e.printStackTrace();  
           }     	
	 }
}
public class HttpServerUDP {
	static SelectARQ selARQ;

	public static void main(String args[]) throws IOException
	{
		 selARQ = new SelectARQ();
		 selARQ.loadConfig();
		 
		 int port= selARQ.serverPortCfg;
         if(args.length==1)
		 try
		 {
			 port=Integer.parseInt(args[0]);
			 if(port<=1000)
				 port=8082;
		 }catch (Exception e)
		 {
			 port=8082;
		 }
         		 
      HttpServerUDP httpserver =new HttpServerUDP();
	  httpserver.Listening(port);
		
	}
	public void Listening(int port)
	{
		try
		{
			System.out.println("server is listening at "+port);
			DatagramSocket socket = new DatagramSocket(port);//receive data port
    		Request request=new Request();
    		Respond respond=new Respond();
    		respond.bindRequest(socket, request);
    		InetAddress destAddr =InetAddress.getByName(selARQ.clientHostCfg);
    		short destPort = (short) selARQ.clientPortCfg;    		
			while(true)
			try{	 
	    		request.GetFieldFromRequest(socket,selARQ);
	    		respond.doRespond(destAddr,destPort,selARQ);
			}catch(Exception e)
			 {
				e.printStackTrace();
				continue;
			 }
			//socket.close();			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
