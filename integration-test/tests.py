# End to End test runner for JSON2Apex, it'll post test json payloads to a locally running
# instance of json2apex, and then validate that they compile file on salesforce

import sys
import beatbox
import xmltramp
import datetime
import zipfile
import os
import urllib
from beatbox_six import BytesIO, http_client

sf = beatbox._tPartnerNS
apex = beatbox._tApexNS
svc = beatbox.Client()

json2apexHost = "localhost:9091"

class JSON2ApexTester:
	def login(self, username, password):
		svc.serverUrl = "https://login.salesforce.com/services/Soap/u/42.0"
		loginResult = svc.login(username, password)
		print("Logged in at {}".format(str(loginResult[sf.serverUrl])))

	def runtests(self):
		skipped = []
		for f in os.listdir("."):
			if f.endswith(".json"):
				if not self.test(f, False):
					return
				if not self.test(f, True):
					return
			elif f.endswith(".skip"):
				skipped.append(f)
		if len(skipped) > 0:
			print("skipped tests {}".format(skipped))
			
	def test(self, json, explicitParse):
		zipf = self.json2apex(json, explicitParse)
		if zipf is None:
			return False
		return self.verifyZip(zipf, json, explicitParse)
		
	def json2apex(self, jsonFile, explicitParse):
		with open(jsonFile, 'r') as j:
			conn = http_client.HTTPConnection(json2apexHost)
			params = {'json':j.read(), "className":"JSON2ApexIntegration", "createParseCode":explicitParse}
			headers = {"Content-Type": "application/x-www-form-urlencoded"}
			body = urllib.parse.urlencode(params)
			conn.request("POST", "/makeApex", body, headers)
			r = conn.getresponse()
			if r.status != 200:
				print("Didn't get expected 200 response from apex2json request")
				print(r.status)
				print(r.getheaders())
				print(r.read())
				return None
			return BytesIO(r.read())
	
	def verifyZip(self, zipData, jsonFile, explicitParse):
		scripts = []
		with zipfile.ZipFile(zipData, 'r') as z:
			for zf in z.infolist():
				with z.open(zf) as c:
					apexCode = c.read()
					scripts.append(apexCode)
					with open("results/{}.{}.{}".format(jsonFile, explicitParse, zf.filename), 'wb') as rf:
						rf.write(apexCode)
		
		print ("Compiling {} scripts generated from {:32s} explicitParse:{:5s}".format(len(scripts), jsonFile, str(explicitParse)), end='')
		res = svc.compileApex(scripts)
		errors = []
		for r in res:
			if str(r[apex.success]) == "false":
				for p in r[apex.problem]:
					errors.append(str(p))
        
		if len(errors) > 0:
			print()
			for s in scripts:
				print(s.decode("utf-8"))
			for e in errors:
				print(" * error ",e)
		else:
			print(" \u2714 success")

		return len(errors) == 0

if __name__ == "__main__":
	
	if len(sys.argv) != 3:
		print ("usage is tests.py <sfdc_username> <sfdc_password>")
	else:
		a = JSON2ApexTester()
		a.login(sys.argv[1], sys.argv[2])
		a.runtests()
