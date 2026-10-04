@echo off
cd /d "C:\Users\miran\Desktop\manager"
"C:\Users\miran\.jdks\ms-21.0.10\bin\javaw.exe" -Djava.awt.headless=false -Dspring.profiles.active=desktop -Dmulticore.demo-login=true -jar "C:\Users\miran\Desktop\manager\desktop\target\multicore-desktop-1.0.0.jar" > "C:\Users\miran\Desktop\manager\desktop.log" 2>&1

