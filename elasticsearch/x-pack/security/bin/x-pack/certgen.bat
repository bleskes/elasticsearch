@echo off
PUSHD "%~dp0"
CALL "%~dp0.in.bat" org.elasticsearch.xpack.security.ssl.CertificateTool %*
POPD
