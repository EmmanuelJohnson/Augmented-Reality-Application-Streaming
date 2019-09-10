#!/usr/bin/env python

import sys, os, time, platform

def main():
  head = "emulator"
  tail = "&"
  kvm = ""
  if platform.system() == "Windows":
    head = "start /B emulator"
    tail = ""
  elif platform.system() == "Linux":
    head = os.popen("which emulator").read().strip("\n")
    kvm = " -qemu -enable-kvm"
  cmd = head + " -avd simulate" + kvm + tail
  print cmd
  os.system(cmd)
  time.sleep(2)


if __name__ == "__main__":
  main()
