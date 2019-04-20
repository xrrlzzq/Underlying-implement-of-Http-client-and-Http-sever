First£¬you need to start Http Server, this server will listen http://localhost:8082, And then you can use Http client or browser to connect to this server

the usage for http client:
java HttpClient Command URL
Command: Post Head GetRequest GetRaw GetHead GetBody GetQuery GetStatus GetCode
Redirection(URL1 sould redirect to URL2 at server side): java HttpClient Redirect URL1

function usage:
http://localhost:port?action=CMD&format=FORMAT&file=FILENAME&overwrite=SWITCH&text=STRING
CMD     : action to operate, include DIR,READ,WRITE,DOWNLOAD
FORMAT  : data format response to client, include JSON,XML,TEXT,HTML
FILENAME: filename to operate
SWITCH  : used for WRITE only, include overwrite=on|off
GET verb(text=STRING): used for WRITE only, STRING will be written to FILENAME
POST verb(<textarea rows=20 cols=30 name="text">STRING</textarea>): used for HTTP POST verb,
use <FORM method="POST"> to submit, STRING will be written to FILENAME