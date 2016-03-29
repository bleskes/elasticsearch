@echo off
PUSHD "%~dp0"
CALL "%~dp0.in.bat" org.elasticsearch.shield.authc.file.tool.UsersTool %*
POPD