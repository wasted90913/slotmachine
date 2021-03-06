<%@ include file="header_static.jsp" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>Slot Mania</title>
    <% if (isWebkit) {%>
    <link rel="stylesheet" href="/wk/css/webkit.css" />    
	<% } else { %>
    <link rel="stylesheet" href="css/wap.css" />    
	<%} %>

	</head>
	<body>
		
		<% if (isWebkit) {%>
		
		<div id="container">
		  	<div class="wrapper">
			    <img style="margin-top:45px;" width="240" height="110" src="images/wk-landing-logo.png"/>
			</div>
		</div>
		<div align="center">Game is available on Feature Phones today.<br/>
							Coming to Smart Phones in early August!</div>
		
		<% } else { %>
		
		<div id="container">
		  	<div class="wrapper">
			    <div class="header-logo">
			   		<h3>Welcome to</h3>
			    	<img width="112" height="34" src="images/logo.gif"/>
			    </div>
				<h3>
					You must be logged in to MocoSpace to play this game!
				</h3>
				<div class="menu">
					<div><a href="<%=GameUtils.getMocoSpaceHome() %>">Login</a></div>
					<div><a href="<%= response.encodeURL("/help.jsp") %>">What is Slot Mania?</a></div>
				</div>
			</div>
		</div>
		
		<%} %>
	<%@ include file="footer.jsp" %>
	</body>
</html>