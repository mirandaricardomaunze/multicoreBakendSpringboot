@echo off
cd /d "C:\Users\miran\Desktop\manager"
"C:\Users\miran\.jdks\ms-21.0.10\bin\java.exe" -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar "C:\Users\miran\Desktop\manager\backend\target\multicore-backend-1.0.0.jar" > "C:\Users\miran\Desktop\manager\backend_run.log" 2>&1
