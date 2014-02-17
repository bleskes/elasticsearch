<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:setBundle basename="prelert_messages" scope="session" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	
	<link rel="stylesheet" type="text/css" href="css/shared/layout.css" />
	
	<link rel="shortcut icon" href="prelert_icon.ico" type="image/x-icon"/>
   	<link rel="icon" href="prelert_icon.ico" type="image/x-icon"/>
	
	<title><fmt:message key="error.title"/></title>
</head>

<body onload="window.focus();">

	<%-- Black header and logo --%>
	<div class="header">
		<div class="block_header">
			<div class="logo">
				<img border=0 alt=logo src="images/shared/2012blacklogo.png"/>
			</div>

		</div>
		<div class="clr"></div>
	</div>

	<%-- Begin Body --%>		
	<div class="body">
		<div class="body_small">
			<div class="centered">
			  <table border="0" cellspacing="5" >
			  
			   	<col style="width:100px;">
				<col style="width:500px;">
				
			    <tr>
				      <th align="right"><img border=0 alt=warning src="images/shared/icon-warning.gif" /></th>
				      <td align="left"><strong><fmt:message key="general_error.message"/></strong></td>
				    </tr>
				    <tr>
				      <th align="right">&nbsp;</th>
				      <td align="left"><fmt:message key="general_error.contactdetails"/></td>
				    </tr>
				    <tr>
				      <th>&nbsp;</th>
				      <td>&nbsp;</td>
				    </tr>
			    
			  </table>
  			</div>
  		</div>
    </div>
	
	<div class="clr"></div>
		
	<%-- Footer with Close button --%>
	<div class="footer">
		<div class="footer_small_blank" style="text-align:right;">
			<input type="image" name="closeBtn" id="closeBtn" src="images/shared/button_close.png" onclick="javascript:window.close();" />
		</div>
	</div>
	
</body>

</html>