import sys
from flask import Flask
from flask_sockets import Sockets
import os, time, platform
import getpass
import telnetlib

app = Flask(__name__)
sockets = Sockets(app)

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

tn = telnetlib.Telnet(host="localhost", port="5554")
authenticate(tn, "bobbyda")


@sockets.route('/accelerometer')
def echo_socket(ws):
	# f=open("accelerometer.txt","a")
	while True:
		message = ws.receive()
		spl = message.split(",")
		values = ":".join(spl)
		tn.write("\n")
		tn.write("sensor set acceleration "+str(values))
		print("Acc : "+values)
		ws.send(message)
		# print>>f,message
	# f.close()


@sockets.route('/gyroscope')
def echo_socket(ws):
	# f=open("gyroscope.txt","a")
	while True:
		message = ws.receive()
		spl = message.split(",")
		values = ":".join(spl)
		tn.write("\n")
		tn.write("sensor set gyroscope-uncalibrated "+str(values))
		tn.write("\n")
		tn.write("sensor set gyroscope 0:0:0")
		print("Gyro : "+message)
		ws.send(message)
	# 	print>>f,message
	# f.close()

@sockets.route('/magnetometer')
def echo_socket(ws):
	# f=open("magnetometer.txt","a")
	while True:
		message = ws.receive()
		spl = message.split(",")
		values = ":".join(spl)
		tn.write("\n")
		tn.write("sensor set magnetic-field-uncalibrated "+str(values))
		print("Magno : "+message)
        ws.send(message)
    #     print>>f,message
	# f.close()

@sockets.route('/orientation')
def echo_socket(ws):
	# f=open("orientation.txt","a")
	while True:
		message = ws.receive()
		spl = message.split(",")
		values = ":".join(spl)
		tn.write("\n")
		print(values)
		tn.write("sensor set orientation "+str(values))
        print("Orient : "+message)
        ws.send(message)
    #   print>>f,message
	# f.close()

@sockets.route('/stepcounter')
def echo_socket(ws):
	f=open("stepcounter.txt","a")
	while True:
		message = ws.receive()
                print(message)
                ws.send(message)
                print>>f,message
	f.close()

@sockets.route('/thermometer')
def echo_socket(ws):
	f=open("thermometer.txt","a")
	while True:
		message = ws.receive()
                print(message)
                ws.send(message)
                print>>f,message
	f.close()

@sockets.route('/lightsensor')
def echo_socket(ws):
	f=open("lightsensor.txt","a")
	while True:
		message = ws.receive()
                print(message)
                ws.send(message)
                print>>f,message
	f.close()

@sockets.route('/proximity')
def echo_socket(ws):
	f=open("proximity.txt","a")
	while True:
		message = ws.receive()
                print(message)
                ws.send(message)
                print>>f,message
	f.close()

@sockets.route('/geolocation')
def echo_socket(ws):
	# f=open("geolocation.txt","a")
	while True:
		message = ws.receive()
        print("Geo : "+message)
        ws.send(message)
    #     print>>f,message
	# f.close()



@app.route('/')
def hello():
	return 'Hello World!'

if __name__ == "__main__":
	from gevent import pywsgi
	from geventwebsocket.handler import WebSocketHandler
	server = pywsgi.WSGIServer(('0.0.0.0', 5000), app, handler_class=WebSocketHandler)
	server.serve_forever()
