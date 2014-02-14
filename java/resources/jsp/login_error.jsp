<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<jsp:useBean id="buildInfo" class="com.prelert.data.BuildInfo" scope="application" />
<fmt:setBundle basename="prelert_messages" scope="session" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	
	<link href="css/shared/layout.css" rel="stylesheet" type="text/css" />
	
	
	<link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon"/>
   	<link rel="icon" href="prelert_icon.ico" type="image/x-icon"/>
   	
   	<title><fmt:message key="login.title"/></title>
</head>

<body>
	<div class="main">
	
		<%-- Black header and logo --%>
		<div class="header">
			<div class="block_header">
				<div class="logo">
					<img border=0 alt=logo src="images/shared/2012blacklogo.png"/>
				</div>

			</div>
			<div class="clr"></div>
		</div>
		
		<%-- Orange Subpage Title Block --%>
		<div class="header_top_subpage">
			<div class=header_top_subpage_resize>
				<h2><fmt:message key="login.login"/></h2>
			</div>
			<div class="clr"></div>
		</div>
		<div class="clr"></div>
		
		<%-- Begin Body --%>		
		<div class="body">
		
			<div class="clr"></div>
			<div class="body_resize">
				<form method="post" action="j_spring_security_check" >
			
					<div class="centered prl-form">
					
					  <table border="0" cellspacing="5" >
					  
					   	<col style="width:100px;">
						<col style="width:245px;">
						<col style="width:130px;">
						
					  	<tr>
					      <th>&nbsp;</th>
					      <td>&nbsp;</td>
					      <td>&nbsp;</td>
					    </tr>
					    <tr>
					      <th align="right"><fmt:message key="login.username"/></th>
					      <td align="left"><input type="text" name="j_username" style="width:240px;"></td>
					      <td class="prl-field-hint"><fmt:message key="login.username.tip"/></td>
					    </tr>
					    <tr>
					      <th align="right"><fmt:message key="login.password"/></th>
					      <td align="left"><input type="password" name="j_password" style="width:240px;"></td>
					      <td class="prl-field-hint"><fmt:message key="login.password.tip"/></td>
					    </tr>
					    <tr>
					      <td align="right">&nbsp;</td>
						  <td align="left"><input type="submit" value="<fmt:message key="login.login"/>"></td>		
						  <td>&nbsp;</td>		      
					    </tr>
					    <tr> 
					      <th align="right" style="color: red;" valign="top"><fmt:message key="login.error"/></th>
			      		  <td colspan="2" align="left" style="color: red;" valign="top"><fmt:message key="login.invalid_password"/></td>
						</tr> 
					    
					  </table>
		 
				  </div>
				</form>
				<div class="clr"></div>
			</div>
			<div class="clr"></div>
			
			
		</div>
		
		<div class="clr"></div>
		
		<%-- Footer --%>
		<div class="footer">
			<div class="footer_resize">
				<p class="leftt"><fmt:message key="version.label"/> <c:out value="${buildInfo.versionNumber}"/>
					<fmt:message key="build.label"/> <c:out value="${buildInfo.buildNumber}"/></p>
				<p class="rightt"><fmt:message key="login.copyright"/> 2006-<c:out value="${buildInfo.buildYear}"/></p>
			</div>
		</div>
		
		<%-- End Main, Body, HTML --%>
	</div>
</body>

</html>
