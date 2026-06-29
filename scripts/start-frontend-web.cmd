@echo off
setlocal
cd /d D:\AIProjects\BookingApi\frontend
npm.cmd run dev -- --host 127.0.0.1 --port 3000 1>D:\AIProjects\BookingApi\logs\frontend.out.log 2>D:\AIProjects\BookingApi\logs\frontend.err.log
