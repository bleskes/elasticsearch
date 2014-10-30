''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
'                                                          '
' Contents of file Copyright (c) Prelert Ltd 2006-2011     '
'                                                          '
'----------------------------------------------------------'
'----------------------------------------------------------'
' WARNING:                                                 '
' THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               '
' SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     '
' PARENT OR SUBSIDIARY COMPANIES.                          '
' PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         '
'                                                          '
' This source code is confidential and any person who      '
' receives a copy of it, or believes that they are viewing '
' it without permission is asked to notify Prelert Ltd     '
' on +44 (0)20 3567 1249 or email to legal@prelert.com.    '
' All intellectual property rights in this source code     '
' are owned by Prelert Ltd.  No part of this source code   '
' may be reproduced, adapted or transmitted in any form or '
' by any means, electronic, mechanical, photocopying,      '
' recording or otherwise.                                  '
'                                                          '
'----------------------------------------------------------'
'                                                          '
'                                                          '
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

'Script to install the Prelert Data Service.

Set oShell = WScript.CreateObject("WScript.Shell")
Set oFileSystem = WScript.CreateObject("Scripting.FileSystemObject")

PrelertHome = oShell.ExpandEnvironmentStrings("%PRELERT_HOME%")
If (PrelertHome = "%PRELERT_HOME%") Then
    'Prelert home is grandparent of this directory
    ScriptDir = oFileSystem.GetParentFolderName(Wscript.ScriptFullName)
    ParentDir = oFileSystem.GetParentFolderName(ScriptDir)
    PrelertHome = oFileSystem.GetParentFolderName(ParentDir)
End If

PgUser = oShell.ExpandEnvironmentStrings("%PGUSER%")
If (PgUser = "%PGUSER%")  Then
    PgUser = oShell.ExpandEnvironmentStrings("%USERNAME%")
End If

Set libFolder = oFileSystem.GetFolder(PrelertHome & "\proxy\lib")

'Add .jar files to the Java ClassPath
ClassPath = PrelertHome & "\proxy\lib\log4j-1.2.16.jar;"
For Each file in libFolder.Files
    If file.name <> "log4j-1.2.16.jar" Then
    	ClassPath = ClassPath & file.Path & ";"
    End If
Next

For Each folder in libFolder.SubFolders
    For Each file in folder.Files
        ClassPath = ClassPath & file.Path & ";"
    Next
Next

ClassPath = ClassPath & PrelertHome & "\config\proxy;"
ClassPath = ClassPath & PrelertHome & "\config\proxy\plugins"

'It is best to fully specify the JVM path
JavaHome = PrelertHome & "\cots\jre"
Jvm = JavaHome & "\bin\server\jvm.dll"
If Not oFileSystem.FileExists(Jvm) Then
    Jvm = JavaHome & "\bin\client\jvm.dll"
    If Not oFileSystem.FileExists(Jvm) Then
        'Last resort - probably will not work
        Jvm = "auto"
    End If
End If
If Jvm <> "auto" Then
    Jvm = """" & Jvm & """"
End If

'Run the install command
installCommand = """" & PrelertHome & "\cots\bin\prunsrv.exe"" //IS/PrelertProxy --DisplayName=""Prelert Proxy"" --Description=""Prelert Proxy Service"" --Install=""" & PrelertHome & "\cots\bin\prunsrv.exe"" ++DependsOn=PostgreSQL --LogPath=""" & PrelertHome & "\logs\proxy"" --JavaHome=""" & JavaHome & """ --Jvm=" & Jvm & " --StartMode=java --StopMode=java --StartClass=com.prelert.proxy.Proxy --StartParams=start --StopClass=com.prelert.proxy.ProxyController ++StopParams=-port ++StopParams=1099 ++StopParams=stop --Classpath=""" & ClassPath & """ ++JvmOptions=-Dpg.user=""" & PgUser & """ ++JvmOptions=-Dprelert.logs=""" & PrelertHome & "\logs"" ++JvmOptions=-Djava.security.policy=""" & PrelertHome & "\config\proxy\java-security.policy"" ++JvmOptions=-Dprelert.config.dir=""" & PrelertHome & "\config\proxy"" --JvmMx 1024"

oShell.Exec installCommand

