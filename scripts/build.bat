@echo off
REM Build DLSCMS on Windows (needs JDK 21+ on PATH). Usage: scripts\build.bat [test]
cd /d "%~dp0\.."
if exist out rmdir /s /q out
mkdir out\classes out\test-classes dist
dir /s /b src\main\java\*.java > out\main-sources.txt
javac --release 17 -encoding UTF-8 -d out\classes @out\main-sources.txt || exit /b 1
echo Main-Class: com.dlscms.Main> out\manifest.txt
jar --create --file dist\dlscms.jar --manifest out\manifest.txt -C out\classes . || exit /b 1
echo BUILD OK -^> dist\dlscms.jar
if /I "%1"=="test" (
  dir /s /b src\test\java\*.java > out\test-sources.txt
  call javac --release 17 -encoding UTF-8 -cp "out\classes;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" -d out\test-classes @out\test-sources.txt || exit /b 1
  java -cp "out\test-classes;out\classes;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" com.dlscms.RunAll out\test-classes
)
