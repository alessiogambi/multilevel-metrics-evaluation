import urllib, urllib2, sys, httplib

url = "/MELA-AnalysisService-1.0/REST_WS"
HOST_IP="localhost"

 

if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP+':8080')
        #read composition rules file
        composition_file = open("./compositionRules.xml", "r")
        body_content =  composition_file.read()
       
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/xml, multipart/related'
	}
 
	connection.request('PUT', url+'/metricscompositionrules', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

