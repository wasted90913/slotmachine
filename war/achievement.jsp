<%@ include file="/header.jsp" %>
<%
	if (!AchievementService.getInstance().isEnabled() && !player.hasAdminPriv()) {
		pageContext.forward("/");
		return;
	}
%>
 
<html xmlns="http://www.w3.org/1999/xhtml">
<!-- index.jsp -->
 <%@ include file="header_html.jsp" %>
	  	
			<div class="content">
				
				<div class="subheader">
		    		Achievements
		    	</div>

		  		<table class="achievement-container">
					<!-- 
		  			<tr class="achievement-entity">
		  				<td></td>
		  				<td>Reward</td>
		  				<td>Granted</td>
		  			</tr>
			  		-->
		  			<% for (Pair<Achievement,Boolean> pair : AchievementService.getInstance().getAchievements(player)) { %>
		  			<tr class="achievement-entity <%= pair.getElement2() ? "" : "disable"%>">
			  			<td class="achievement-icon"><img width="24" height="24" src="<%= pair.getElement1().getType().getWAPImage() %>"></td>
		  				<td class="achievement"><%= pair.getElement1().getTitle() %></td>
		  				<td class="achievement-coins"><%= pair.getElement1().getCoinsAwarded() %> coins</td>
		  			</tr>
			   		<% } %>
		  		</table>	
		  			   
		  		<div class="menu">
		  			<div><a href="<%= ServletUtils.buildUrl(player, "/", response) %>">Main</a></div>
		  		</div>
  		
  			</div>
	  		
		</div>
	</div>
	<%@ include file="footer.jsp" %>
  </body>
</html>