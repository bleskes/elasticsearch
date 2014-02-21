@echo off
rem ############################################################
rem #                                                          #
rem # Contents of file Copyright (c) Prelert Ltd 2006-2012     #
rem #                                                          #
rem #----------------------------------------------------------#
rem #----------------------------------------------------------#
rem # WARNING:                                                 #
rem # THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               #
rem # SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     #
rem # PARENT OR SUBSIDIARY COMPANIES.                          #
rem # PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         #
rem #                                                          #
rem # This source code is confidential and any person who      #
rem # receives a copy of it, or believes that they are viewing #
rem # it without permission is asked to notify Prelert Ltd     #
rem # on +44 (0)20 7953 7243 or email to legal@prelert.com.    #
rem # All intellectual property rights in this source code     #
rem # are owned by Prelert Ltd.  No part of this source code   #
rem # may be reproduced, adapted or transmitted in any form or #
rem # by any means, electronic, mechanical, photocopying,      #
rem # recording or otherwise.                                  #
rem #                                                          #
rem #----------------------------------------------------------#
rem #                                                          #
rem #                                                          #
rem ############################################################

setlocal

rem Guess PRELERT_HOME if not defined
if defined PRELERT_HOME goto gotHome
set PRELERT_HOME=%~dp0\..\..

:gotHome
echo Using PRELERT_HOME = %PRELERT_HOME%

set PRELERT_JRE_HOME=%PRELERT_HOME%\cots\jre\bin

if not defined PGUSER set PGUSER=%USERNAME%

"%PRELERT_JRE_HOME%\java" -Dpg.user=%PGUSER% -Dprelert.home="%PRELERT_HOME%" -Dprelert.logs="%PRELERT_HOME%\logs" -Dprelert.config.dir="%PRELERT_HOME%\config\proxy" -Djava.security.policy="%PRELERT_HOME%\config\proxy\java-security.policy" -classpath "%PRELERT_HOME%\proxy\lib\log4j-1.2.16.jar;%PRELERT_HOME%\config\proxy;%PRELERT_HOME%\config\proxy\plugins;%PRELERT_HOME%\proxy\lib\*" com.prelert.proxy.Proxy

