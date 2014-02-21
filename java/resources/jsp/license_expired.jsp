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
   	
   	<title><fmt:message key="license_expired.title"/></title>
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
				<h2><fmt:message key="license_expired.heading"/></h2>
			</div>
			<div class="clr"></div>
		</div>
		<div class="clr"></div>
		
		<%-- Begin Body --%>		
		<div class="body">
			<div class="body_resize">
				<div class="left">
					  <table border="0" cellspacing="5" >
					  
					   	<col style="width:100px;">
						<col style="width:350px;">
						
					    <tr>
					      <th align="right"><img border=0 alt=warning src="images/shared/icon-warning.gif" /></th>
					      <td align="left"><strong><fmt:message key="license_expired.expired"/></strong></td>
					    </tr>
					    <tr>
					      <th align="right">&nbsp;</th>
					      <td align="left"><fmt:message key="license_expired.contactdetails"/></td>
					    </tr>
					    <tr>
					      <th>&nbsp;</th>
					      <td>&nbsp;</td>
					    </tr>
					    
					  </table>
	
				</div>
		
	    		<div class="right">
					<p><strong><fmt:message key="contact_support.phone"/></strong><br />
						<fmt:message key="contact_support.phone1"/><br />
						<fmt:message key="contact_support.phone2"/>
					</p>
	    		</div>
	    		<div class="clr"></div>
	  
	  		</div>
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
