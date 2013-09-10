import urllib, urllib2, sys, httplib

url = "/MELA-AnalysisService-1.0/REST_WS"
HOST_IP="localhost"

 

if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP+':8080')
        description_file = open("./serviceDescription.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', url+'/servicedescription', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

