<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:setBundle basename="prelert_messages" scope="session" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	
	<link href="css/shared/layout.css" rel="stylesheet" type="text/css" />
	
	<title><fmt:message key="contact_support.title"/></title>
</head>

<body onload="window.focus();">

	<%-- Black header and logo --%>
	<div class="header">
		<div class="block_header">
			<div class="logo">
				<img border="0" alt="logo" src="images/shared/2012blacklogo.png"/>
			</div>

		</div>
		<div class="clr"></div>
	</div>


	<%-- Begin Body --%>		
	<div class="body">
		<div class="body_small">
			<div class="left_narrow">
				<p><fmt:message key="contact_support.text"/></p>
			</div>
	
    		<div class="right_narrow">
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
		<div class="footer_small_blank" style="text-align:right;">
			<input type="image" name="closeBtn" id="closeBtn" src="images/shared/button_close.png" onclick="javascript:window.close();" />
		</div>
	</div>

</body>

</html>