<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<jsp:useBean id="buildInfo" class="com.prelert.web.BuildInfo" scope="application" />
<fmt:setBundle basename="prelert_messages" scope="session" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	
	<link href="css/layout.css" rel="stylesheet" type="text/css" />
	
	
	<link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon"/>
   	<link rel="icon" href="prelert_icon.ico" type="image/x-icon"/>
   	
   	<title><fmt:message key="login.title"/></title>
</head>

<body>
<div id="container">
	
		<div style="height: 10%;">
	
		</div>
		
		<div id="top">
			<img src="images/header_logo.png" width="194" height="70" class="no_border" id="prelert_logo" alt="Prelert"/>
		</div>
		
		<div id="middle">
	
		    <div id="body_top"><img src="images/login_header.png" width="530" height="70" alt="<fmt:message key="login.login"/>" /></div>
		    
		    <div id="body"> 
		    
		    	<form method="post" action="<c:url value='j_spring_security_check'/>" >
				
					<div class="centered prl-form">
					
					  <table border="0" cellspacing="5" >
					  
					   	<col style="width:100px;"/>
						<col style="width:350px;"/>
						
					  	<tr>
					      <th>&nbsp;</th>
					      <td>&nbsp;</td>
					    </tr>
					    <tr>
					      <th align="right"><fmt:message key="login.username"/></th>
					      <td align="left"><input type="text" name="j_username"></td>
					    </tr>
					    <tr>
					      <th align="right"><fmt:message key="login.password"/></th>
					      <td align="left"><input type="password" name="j_password"></td>
					    </tr>
					    <tr>
					      <td align="right"><input type="submit" value="<fmt:message key="login.login"/>"></td>
					      <td align="left"><input type="reset"></td>
					    </tr>
					    <tr height="100%">
					      <th align="right" style="color: red;" valign="top"><fmt:message key="login.error"/></th>
			      		  <td align="left" style="color: red;" valign="top"><fmt:message key="login.invalid_password"/></td>
					    </tr>
					    
					  </table>
		 
				  </div>
				</form>
		    
		    </div>
		    
		    <div id="body_end">
		    	<div style="position:relative;top:75px;" class="centered prl-footerText" >
		    		<fmt:message key="version.label"/> <c:out value="${buildInfo.versionNumber}"/> 
		    			<fmt:message key="build.label"/> <c:out value="${buildInfo.buildNumber}"/>
		    	</div>
		    </div>

		</div>
		
		<div id="bottom" class="centered prl-footerText">
			<fmt:message key="login.copyright"/> <c:out value="${buildInfo.buildYear}"/>
		</div>
	</div>
</body>

</html>