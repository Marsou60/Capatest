@ECHO OFF
set DIR=%~dp0
set CLASSPATH=%DIR%\gradle\wrapper\gradle-wrapper.jar
java -jar %CLASSPATH% %*
