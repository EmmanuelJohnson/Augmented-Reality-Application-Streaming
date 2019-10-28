#!/usr/bin/env python

import sys, os, time, platform
import getpass
import telnetlib
from random import randrange

# To authenticate the user in telnet
def authenticate(telnet, userName):
	try:
		print "Authenticating the user"
		authFile = open('/Users/'+userName+'/.emulator_console_auth_token', 'r')
		getAuth = authFile.read()
		telnet.write("\n")
		authenticate = "auth " + getAuth
		telnet.write(authenticate)
		time.sleep(1)
		telnet.write("\n")
		print "User Authenticated"
	except Exception as e:
		print (e)
		print "Error in Authenticating"

import csv

def getCsvValues(filePath):
	print "Parsing the CSV file : "+filePath
	values = list()
	with open(filePath) as csvFile:
		csvReader = csv.reader(csvFile, delimiter=',')
		lineCount = 0
		for row in csvReader:
			# Header Row
			if lineCount == 0:
				lineCount += 1
			# Value Rows
			else:
				data = dict()
				data['timestamp'] = row[0]
				data['longitude'] = row[1]
				data['latitude'] = row[2]
				data['rotX'] = row[3]
				data['rotY'] = row[4]
				data['rotZ'] = row[5]
				data['rotW'] = row[6]
				data['accX'] = row[10]
				data['accY'] = row[11]
				data['accZ'] = row[12]
				data['gyroX'] = row[14]
				data['gyroY'] = row[15]
				data['gyroZ'] = row[16]
				values.append(data)
				lineCount += 1
		print "Processed "+str(lineCount)+" lines"
		return values

def simulate(telnet, simulationValues):
	try:
		total = len(simulationValues)
		s = 0
		while s < total:
			data = simulationValues[s]
			print "Executing "
			print data
			accelValues = str(float(data['accX']))+":"+str(float(data['accY']))+":"+str(float(data['accZ']))
			telnet.write("\n")
			telnet.write("sensor set acceleration "+accelValues)
			telnet.write("\n")
			gyroValues = str(float(data['gyroX']))+":"+str(float(data['gyroY']))+":"+str(float(data['gyroZ']))
			telnet.write("sensor set gyroscope "+gyroValues)
			telnet.write("\n")
			telnet.write("sensor set gyroscope 0:0:0")
			time.sleep(0.1)
	except KeyboardInterrupt:
		print "Exiting"

def main():
	if len(sys.argv) != 3:
		print "Please enter the username and the csv file path"

	# Get the username and the csv file path
	userName = sys.argv[1]
	csvFile = sys.argv[2]

	print "Establishing Connection"
	tn = telnetlib.Telnet(host="localhost", port="5554")
	authenticate(tn, userName)

	# Get the simulation values from the csv file
	simulationValues = getCsvValues(csvFile)
	
	print "Simulating Gyroscope Sensor"
	simulate(tn, simulationValues)

	print "Exiting Simulation"
	tn.write("\n")
	tn.write("exit")

if __name__ == "__main__":
  main()
