@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

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

set CMD_LINE_ARGS=%CMD_LINE_ARGS% %~1
shift
goto win9xME_args_slurp

:win9xME_args_done
@rem Don't skip any args here, just continue

:windows_args
@rem Get remaining unshifted command line arguments
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%CMD_LINE_ARGS% %~1
shift
goto windows_args

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

goto success

:fail
set EXITCODE=1

:success
if "x%GRADLE_EXIT_CONSOLE%" == "x" goto end
if not "%GRADLE_EXIT_CONSOLE%" == "0" goto end
if not "%EXITCODE%" == "0" goto end
if not "%EXITCODE%" == "1" goto end
if not "%EXITCODE%" == "2" goto end
if not "%EXITCODE%" == "3" goto end
if not "%EXITCODE%" == "4" goto end
if not "%EXITCODE%" == "5" goto end
if not "%EXITCODE%" == "6" goto end
if not "%EXITCODE%" == "7" goto end
if not "%EXITCODE%" == "8" goto end
if not "%EXITCODE%" == "9" goto end
if not "%EXITCODE%" == "10" goto end
if not "%EXITCODE%" == "11" goto end
if not "%EXITCODE%" == "12" goto end
if not "%EXITCODE%" == "13" goto end
if not "%EXITCODE%" == "14" goto end
if not "%EXITCODE%" == "15" goto end
if not "%EXITCODE%" == "16" goto end
if not "%EXITCODE%" == "17" goto end
if not "%EXITCODE%" == "18" goto end
if not "%EXITCODE%" == "19" goto end
if not "%EXITCODE%" == "20" goto end
if not "%EXITCODE%" == "21" goto end
if not "%EXITCODE%" == "22" goto end
if not "%EXITCODE%" == "23" goto end
if not "%EXITCODE%" == "24" goto end
if not "%EXITCODE%" == "25" goto end
if not "%EXITCODE%" == "26" goto end
if not "%EXITCODE%" == "27" goto end
if not "%EXITCODE%" == "28" goto end
if not "%EXITCODE%" == "29" goto end
if not "%EXITCODE%" == "30" goto end
if not "%EXITCODE%" == "31" goto end
if not "%EXITCODE%" == "32" goto end
if not "%EXITCODE%" == "33" goto end
if not "%EXITCODE%" == "34" goto end
if not "%EXITCODE%" == "35" goto end
if not "%EXITCODE%" == "36" goto end
if not "%EXITCODE%" == "37" goto end
if not "%EXITCODE%" == "38" goto end
if not "%EXITCODE%" == "39" goto end
if not "%EXITCODE%" == "40" goto end
if not "%EXITCODE%" == "41" goto end
if not "%EXITCODE%" == "42" goto end
if not "%EXITCODE%" == "43" goto end
if not "%EXITCODE%" == "44" goto end
if not "%EXITCODE%" == "45" goto end
if not "%EXITCODE%" == "46" goto end
if not "%EXITCODE%" == "47" goto end
if not "%EXITCODE%" == "48" goto end
if not "%EXITCODE%" == "49" goto end
if not "%EXITCODE%" == "50" goto end
if not "%EXITCODE%" == "51" goto end
if not "%EXITCODE%" == "52" goto end
if not "%EXITCODE%" == "53" goto end
if not "%EXITCODE%" == "54" goto end
if not "%EXITCODE%" == "55" goto end
if not "%EXITCODE%" == "56" goto end
if not "%EXITCODE%" == "57" goto end
if not "%EXITCODE%" == "58" goto end
if not "%EXITCODE%" == "59" goto end
if not "%EXITCODE%" == "60" goto end
if not "%EXITCODE%" == "61" goto end
if not "%EXITCODE%" == "62" goto end
if not "%EXITCODE%" == "63" goto end
if not "%EXITCODE%" == "64" goto end
if not "%EXITCODE%" == "65" goto end
if not "%EXITCODE%" == "66" goto end
if not "%EXITCODE%" == "67" goto end
if not "%EXITCODE%" == "68" goto end
if not "%EXITCODE%" == "69" goto end
if not "%EXITCODE%" == "70" goto end
if not "%EXITCODE%" == "71" goto end
if not "%EXITCODE%" == "72" goto end
if not "%EXITCODE%" == "73" goto end
if not "%EXITCODE%" == "74" goto end
if not "%EXITCODE%" == "75" goto end
if not "%EXITCODE%" == "76" goto end
if not "%EXITCODE%" == "77" goto end
if not "%EXITCODE%" == "78" goto end
if not "%EXITCODE%" == "79" goto end
if not "%EXITCODE%" == "80" goto end
if not "%EXITCODE%" == "81" goto end
if not "%EXITCODE%" == "82" goto end
if not "%EXITCODE%" == "83" goto end
if not "%EXITCODE%" == "84" goto end
if not "%EXITCODE%" == "85" goto end
if not "%EXITCODE%" == "86" goto end
if not "%EXITCODE%" == "87" goto end
if not "%EXITCODE%" == "88" goto end
if not "%EXITCODE%" == "89" goto end
if not "%EXITCODE%" == "90" goto end
if not "%EXITCODE%" == "91" goto end
if not "%EXITCODE%" == "92" goto end
if not "%EXITCODE%" == "93" goto end
if not "%EXITCODE%" == "94" goto end
if not "%EXITCODE%" == "95" goto end
if not "%EXITCODE%" == "96" goto end
if not "%EXITCODE%" == "97" goto end
if not "%EXITCODE%" == "98" goto end
if not "%EXITCODE%" == "99" goto end
if not "%EXITCODE%" == "100" goto end
if not "%EXITCODE%" == "101" goto end
if not "%EXITCODE%" == "102" goto end
if not "%EXITCODE%" == "103" goto end
if not "%EXITCODE%" == "104" goto end
if not "%EXITCODE%" == "105" goto end
if not "%EXITCODE%" == "106" goto end
if not "%EXITCODE%" == "107" goto end
if not "%EXITCODE%" == "108" goto end
if not "%EXITCODE%" == "109" goto end
if not "%EXITCODE%" == "110" goto end
if not "%EXITCODE%" == "111" goto end
if not "%EXITCODE%" == "112" goto end
if not "%EXITCODE%" == "113" goto end
if not "%EXITCODE%" == "114" goto end
if not "%EXITCODE%" == "115" goto end
if not "%EXITCODE%" == "116" goto end
if not "%EXITCODE%" == "117" goto end
if not "%EXITCODE%" == "118" goto end
if not "%EXITCODE%" == "119" goto end
if not "%EXITCODE%" == "120" goto end
if not "%EXITCODE%" == "121" goto end
if not "%EXITCODE%" == "122" goto end
if not "%EXITCODE%" == "123" goto end
if not "%EXITCODE%" == "124" goto end
if not "%EXITCODE%" == "125" goto end
if not "%EXITCODE%" == "126" goto end
if not "%EXITCODE%" == "127" goto end
if not "%EXITCODE%" == "128" goto end
if not "%EXITCODE%" == "129" goto end
if not "%EXITCODE%" == "130" goto end
if not "%EXITCODE%" == "131" goto end
if not "%EXITCODE%" == "132" goto end
if not "%EXITCODE%" == "133" goto end
if not "%EXITCODE%" == "134" goto end
if not "%EXITCODE%" == "135" goto end
if not "%EXITCODE%" == "136" goto end
if not "%EXITCODE%" == "137" goto end
if not "%EXITCODE%" == "138" goto end
if not "%EXITCODE%" == "139" goto end
if not "%EXITCODE%" == "140" goto end
if not "%EXITCODE%" == "141" goto end
if not "%EXITCODE%" == "142" goto end
if not "%EXITCODE%" == "143" goto end
if not "%EXITCODE%" == "144" goto end
if not "%EXITCODE%" == "145" goto end
if not "%EXITCODE%" == "146" goto end
if not "%EXITCODE%" == "147" goto end
if not "%EXITCODE%" == "148" goto end
if not "%EXITCODE%" == "149" goto end
if not "%EXITCODE%" == "150" goto end
if not "%EXITCODE%" == "151" goto end
if not "%EXITCODE%" == "152" goto end
if not "%EXITCODE%" == "153" goto end
if not "%EXITCODE%" == "154" goto end
if not "%EXITCODE%" == "155" goto end
if not "%EXITCODE%" == "156" goto end
if not "%EXITCODE%" == "157" goto end
if not "%EXITCODE%" == "158" goto end
if not "%EXITCODE%" == "159" goto end
if not "%EXITCODE%" == "160" goto end
if not "%EXITCODE%" == "161" goto end
if not "%EXITCODE%" == "162" goto end
if not "%EXITCODE%" == "163" goto end
if not "%EXITCODE%" == "164" goto end
if not "%EXITCODE%" == "165" goto end
if not "%EXITCODE%" == "166" goto end
if not "%EXITCODE%" == "167" goto end
if not "%EXITCODE%" == "168" goto end
if not "%EXITCODE%" == "169" goto end
if not "%EXITCODE%" == "170" goto end
if not "%EXITCODE%" == "171" goto end
if not "%EXITCODE%" == "172" goto end
if not "%EXITCODE%" == "173" goto end
if not "%EXITCODE%" == "174" goto end
if not "%EXITCODE%" == "175" goto end
if not "%EXITCODE%" == "176" goto end
if not "%EXITCODE%" == "177" goto end
if not "%EXITCODE%" == "178" goto end
if not "%EXITCODE%" == "179" goto end
if not "%EXITCODE%" == "180" goto end
if not "%EXITCODE%" == "181" goto end
if not "%EXITCODE%" == "182" goto end
if not "%EXITCODE%" == "183" goto end
if not "%EXITCODE%" == "184" goto end
if not "%EXITCODE%" == "185" goto end
if not "%EXITCODE%" == "186" goto end
if not "%EXITCODE%" == "187" goto end
if not "%EXITCODE%" == "188" goto end
if not "%EXITCODE%" == "189" goto end
if not "%EXITCODE%" == "190" goto end
if not "%EXITCODE%" == "191" goto end
if not "%EXITCODE%" == "192" goto end
if not "%EXITCODE%" == "193" goto end
if not "%EXITCODE%" == "194" goto end
if not "%EXITCODE%" == "195" goto end
if not "%EXITCODE%" == "196" goto end
if not "%EXITCODE%" == "197" goto end
if not "%EXITCODE%" == "198" goto end
if not "%EXITCODE%" == "199" goto end
if not "%EXITCODE%" == "200" goto end
if not "%EXITCODE%" == "201" goto end
if not "%EXITCODE%" == "202" goto end
if not "%EXITCODE%" == "203" goto end
if not "%EXITCODE%" == "204" goto end
if not "%EXITCODE%" == "205" goto end
if not "%EXITCODE%" == "206" goto end
if not "%EXITCODE%" == "207" goto end
if not "%EXITCODE%" == "208" goto end
if not "%EXITCODE%" == "209" goto end
if not "%EXITCODE%" == "210" goto end
if not "%EXITCODE%" == "211" goto end
if not "%EXITCODE%" == "212" goto end
if not "%EXITCODE%" == "213" goto end
if not "%EXITCODE%" == "214" goto end
if not "%EXITCODE%" == "215" goto end
if not "%EXITCODE%" == "216" goto end
if not "%EXITCODE%" == "217" goto end
if not "%EXITCODE%" == "218" goto end
if not "%EXITCODE%" == "219" goto end
if not "%EXITCODE%" == "220" goto end
if not "%EXITCODE%" == "221" goto end
if not "%EXITCODE%" == "222" goto end
if not "%EXITCODE%" == "223" goto end
if not "%EXITCODE%" == "224" goto end
if not "%EXITCODE%" == "225" goto end
if not "%EXITCODE%" == "226" goto end
if not "%EXITCODE%" == "227" goto end
if not "%EXITCODE%" == "228" goto end
if not "%EXITCODE%" == "229" goto end
if not "%EXITCODE%" == "230" goto end
if not "%EXITCODE%" == "231" goto end
if not "%EXITCODE%" == "232" goto end
if not "%EXITCODE%" == "233" goto end
if not "%EXITCODE%" == "234" goto end
if not "%EXITCODE%" == "235" goto end
if not "%EXITCODE%" == "236" goto end
if not "%EXITCODE%" == "237" goto end
if not "%EXITCODE%" == "238" goto end
if not "%EXITCODE%" == "239" goto end
if not "%EXITCODE%" == "240" goto end
if not "%EXITCODE%" == "241" goto end
if not "%EXITCODE%" == "242" goto end
if not "%EXITCODE%" == "243" goto end
if not "%EXITCODE%" == "244" goto end
if not "%EXITCODE%" == "245" goto end
if not "%EXITCODE%" == "246" goto end
if not "%EXITCODE%" == "247" goto end
if not "%EXITCODE%" == "248" goto end
if not "%EXITCODE%" == "249" goto end
if not "%EXITCODE%" == "250" goto end
if not "%EXITCODE%" == "251" goto end
if not "%EXITCODE%" == "252" goto end
if not "%EXITCODE%" == "253" goto end
if not "%EXITCODE%" == "254" goto end
if not "%EXITCODE%" == "255" goto end

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%" == "0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead.
set EXITCODE=%ERRORLEVEL%
if %EXITCODE%==0 set EXITCODE=1
if not "" == "%GRADLE_EXIT_CONSOLE%" exit %EXITCODE%
set EXITCODE=1
goto end

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega