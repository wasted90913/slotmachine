<%@page import="com.sun.java_cup.internal.runtime.Symbol"%>
<%@ include file="header.jsp" %>
<%@ page import="com.solitude.slots.service.SlotMachineManager.InsufficientFundsException" %>
 

<%
String action = request.getParameter("action");
SpinResult spinResult = new SpinResult(-1,new int[]{-1,-1,-1});
int symbol[];
boolean fSpinOK = false;

try {
	if ("spin".equals(action)) {
		spinResult = SlotMachineManager.getInstance().spin(player, 1);
	} else if ("maxspin".equals(action)) {
		spinResult = SlotMachineManager.getInstance().spin(player, Integer.parseInt(System.getProperty("game.max.bet.coins")));
	}
	
	fSpinOK=true;
} catch (InsufficientFundsException ife) {
	//use redirect to ensure the logging works for topup impressions
	
	response.sendRedirect("/wk/topup.jsp?notifymsg="+java.net.URLEncoder.encode("You need more coins to spin!","UTF-8"));
	return;
}
symbol= spinResult.getSymbols(); //always initialize so if fSpinOK flase still get valid symbols to display
int key = 1;

%>
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ include file="header_html.jsp" %>
	<% if (action != null) { %>
	  <script>
	  	document.addEventListener('load',function() {
	  		setTimeout(function() {
	  			document.getElementById('spin-animation-1').style.display = 'none';
	 	   		document.getElementById('spin-animation-2').style.display = 'none';
	 	    	document.getElementById('spin-animation-3').style.display = 'none';
	  			// make any item with class 'delay' visible after 1 second
	  	  		var elems = document.querySelectorAll('.delay');
	  	  		if (elems) {
	  	  			for (var i=0;i<elems.length;i++) {
	  	  				elems[i].style.display = 'block';
	  	  			}
	  	  		}  	  		
	  		}, 750);  		
	  	},true);
	  </script>
	<% } %>
  <body>
  	<div id="container">
	  	<div class="wrapper">
		    <div class="header-logo"><img width="192" height="60" src="images/logo.png"/></div>
		    
		    <div class="content">
			    
			    <table class="stats">
			    	<tr>
						<td>
							<b>XP:</b> <%= player.getXp() %>
						</td>
						<td>
							<b>Coins:</b> <%= player.getCoins() %>
						</td>
					</tr>
				</table>
				
				<div class="results-container">
					<div class="results">
						<% if (fSpinOK==true) {
							
							//only for maxspin give ability to win Moco Gold
							if ("maxspin".equals(action) && symbol[0]==0 && symbol[1]==0 && symbol[2]==0) {
						%>
							<div class="goldtext">
								You WON the Moco Gold Jackpot<br />
								<img width="112" height="14" src="images/jackpot-winner.gif"/>
							</div>		
						<%		
							} else if (spinResult.getCoins()>0) {
						%>
							<div class="wonspin delay" style="display:none;">
								<img width="13" height="11" src="images/animated-star.gif"/>WON <%=spinResult.getCoins() %> coins! <img width="13" height="11" src="images/animated-star.gif"/>
							</div>
								
						<%		} else if (spinResult.getCoins()==0) { %>
							<div class="lostspin delay" style="display:none;">
								Spin again!
							</div>
						<% 		}
							} else  { %>
							<div class="nofunds">
							You have no coins!
							</div>
						<% } %>
					</div>
				</div>
				
				<div class="spin-container">
					
					<% if (fSpinOK==true) {
							
						//only for maxspin give ability to win Moco Gold
						if ("maxspin".equals(action) && symbol[0]==0 && symbol[1]==0 && symbol[2]==0) {
					%>
					
						<div class="lights-win">
							<div class="lights left"></div>
							<div class="lights right"></div>
							<div class="lights top"></div>
							<div class="lights bottom"></div>
						</div>
					
					<%		
						} else if (spinResult.getCoins()>0) {
					%>
					
						<div class="lights-win">
							<div class="lights left"></div>
							<div class="lights right"></div>
							<div class="lights top"></div>
							<div class="lights bottom"></div>
						</div>
					
					<%		} else if (spinResult.getCoins()==0) { %>
					
						<div class="lights-off">
							<div class="lights left"></div>
							<div class="lights right"></div>
							<div class="lights top"></div>
							<div class="lights bottom"></div>
						</div>
					
					<% 		}
						} else  { %>
						
						<div class="lights-off">
							<div class="lights left"></div>
							<div class="lights right"></div>
							<div class="lights top"></div>
							<div class="lights bottom"></div>
						</div>
							
					<% } %>
					
					<div class="lights-off">
						<div class="lights left"></div>
						<div class="lights right"></div>
						<div class="lights top"></div>
						<div class="lights bottom"></div>
					</div>
					
					<table class="spins">
						<tr >
							<td>
								<div>
									<% if (action != null) { %>
									<span id="spin-animation-1">
										<span class="shadows"></span>
										<img src="/wk/images/spin-static-animation.jpg" alt="animacion" width="70" height="249"/>
									</span>
									<% } %>
									<img width="70" height="102" src="/wk/<%=imageLocation+"comb-"+symbol[0] %>.jpg"/>
								</div>
							</td>
							<td>
								<div>
									<% if (action != null) { %>
									<span id="spin-animation-2">
										<span class="shadows"></span>
										<img src="/wk/images/spin-static-animation.jpg" alt="animacion" width="70" height="249" />
									</span>
									<% } %>
									<img width="70" height="102" src="/wk/<%=imageLocation+"comb-"+symbol[1] %>.jpg"/>
								</div>
							</td>
							<td>
								<div>
									<% if (action != null) { %>	
									<span id="spin-animation-3">
										<span class="shadows"></span>
										<img src="/wk/images/spin-static-animation.jpg" alt="animacion" width="70" height="249" />
									</span>
									<% } %>
									<img width="70" height="102" src="/wk/<%=imageLocation+"comb-"+symbol[2] %>.jpg"/>
								</div>
							</td>
						</tr>
					</table>
					
				</div>	
				
				<div class="controls">
					
					<% if (fSpinOK==true) { %>
					
					<table class="bets" align="center" >
						<tr>
							<td class="bet-1">
								<a class="bet" href="<%= response.encodeURL("/wk/spin.jsp?action=spin&"+cacheBuster) %>"> BET 1</a>
							</td>
							<td class="bet-2">
								<a class="bet" href="<%= response.encodeURL("/wk/spin.jsp?action=maxspin&"+cacheBuster) %>"> BET 3</a>
							</td>
						</tr>
					</table>
					<% } %>
	
				     <table class="menu">
				        <tr>
				        	<td>
				        		<a href="<%= response.encodeURL("/wk/index.jsp") %>">Main</a>
				        	</td>
				        	<td>
				        		<a accessKey="2" href="<%= response.encodeURL("/wk/topup.jsp") %>">Buy Coins</a>
				        	</td>
				        </tr>
				    </table>
				</div>
			</div>
		</div>
	</div>
  </body>
</html>
