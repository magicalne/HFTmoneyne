@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  HFTmoneyne-exchange startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and HF_TMONEYNE_EXCHANGE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\netty-codec-4.1.33.Final.jar;%APP_HOME%\lib\HFTmoneyne-exchange.jar;%APP_HOME%\lib\reactive-streams-1.0.2.jar;%APP_HOME%\lib\javax.activation-1.2.0.jar;%APP_HOME%\lib\jackson-annotations-2.9.0.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.33.Final.jar;%APP_HOME%\lib\HFTmoneyne-common.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.33.Final.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\netty-buffer-4.1.33.Final.jar;%APP_HOME%\lib\annotations-17.0.0.jar;%APP_HOME%\lib\compiler-2.3.4.jar;%APP_HOME%\lib\async-http-client-netty-utils-2.8.1.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.33.Final.jar;%APP_HOME%\lib\netty-transport-4.1.33.Final.jar;%APP_HOME%\lib\chronicle-core-2.17.6.jar;%APP_HOME%\lib\affinity-3.1.11.jar;%APP_HOME%\lib\disruptor-3.4.2.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\checker-qual-2.5.2.jar;%APP_HOME%\lib\jna-4.2.1.jar;%APP_HOME%\lib\netty-codec-http-4.1.33.Final.jar;%APP_HOME%\lib\annotations-12.0.jar;%APP_HOME%\lib\netty-reactive-streams-2.0.0.jar;%APP_HOME%\lib\netty-codec-socks-4.1.33.Final.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\netty-common-4.1.33.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.33.Final.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\commons-collections4-4.1.jar;%APP_HOME%\lib\error_prone_annotations-2.2.0.jar;%APP_HOME%\lib\chronicle-threads-2.17.2.jar;%APP_HOME%\lib\jna-platform-4.2.1.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\jackson-databind-2.9.6.jar;%APP_HOME%\lib\netty-handler-4.1.33.Final.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\async-http-client-2.8.1.jar;%APP_HOME%\lib\netty-resolver-4.1.33.Final.jar;%APP_HOME%\lib\jackson-core-2.9.6.jar;%APP_HOME%\lib\guava-27.0.1-jre.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.17.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.33.Final-linux-x86_64.jar;%APP_HOME%\lib\chronicle-wire-2.17.27.jar;%APP_HOME%\lib\chronicle-bytes-2.17.18.jar;%APP_HOME%\lib\netty-all-4.1.32.Final.jar;%APP_HOME%\lib\HFTmoneyne-trade.jar

@rem Execute HFTmoneyne-exchange
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %HF_TMONEYNE_EXCHANGE_OPTS%  -classpath "%CLASSPATH%" magicalne.github.io.Application %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable HF_TMONEYNE_EXCHANGE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%HF_TMONEYNE_EXCHANGE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
