#!/usr/bin/env python

import sys, os, time, platform
import getpass
import telnetlib
from random import randrange

def main():
	if len(sys.argv) != 2:
		print "Please enter the username"
	username = sys.argv[1]
	print "Establishing Connection"
	tn = telnetlib.Telnet(host="localhost", port="5554")
	print "Authenticating the user"
	authFile = open('/Users/'+username+'/.emulator_console_auth_token', 'r')
	getAuth = authFile.read()
	authenticate = "auth " + getAuth +"\n"
	tn.write(authenticate)
	print "User Authenticated"
	print "Simulating Gyroscope Sensor"
	try:
		while True:
			gyroValues = str(randrange(100))+":"+str(randrange(100))+":"+str(randrange(100))
			print "Values : "+gyroValues
			tn.write("sensor set gyroscope "+gyroValues+"\n")
			time.sleep(2)
	except KeyboardInterrupt:
		print "Exiting"
		tn.write("exit\n")

if __name__ == "__main__":
  main()
