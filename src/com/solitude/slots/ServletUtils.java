package com.solitude.slots;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.solitude.slots.entities.Player;

/**
 * Holds servlet utility methods
 * @author kwright
 */
public class ServletUtils {
	
	/** private constructor to ensure static only methods */
	private ServletUtils() {}
	
	/**
	 * @param request object
	 * @param paramName of parameter to extract
	 * @return parameter value as integer or -1 if missing or not valid integer
	 */
	public static int getInt(HttpServletRequest request, String paramName) {
		return getInt(request,paramName,-1);
	}

	/**
	 * @param request object
	 * @param paramName of parameter to extract
	 * @param defaultVal returned if parameter is missing or not a valid integer
	 * @return parameter value as integer or defaultVal
	 */
	public static int getInt(HttpServletRequest request, String paramName, int defaultVal) {
		try {
			return Integer.parseInt(request.getParameter(paramName));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
	
	/**
	 * @param request object
	 * @param paramName of parameter to extract
	 * @return parameter value as long or -1 if missing or not valid long
	 */
	public static long getLong(HttpServletRequest request, String paramName) {
		return getLong(request,paramName,-1L);
	}
	
	/**
	 * @param request object
	 * @param paramName of parameter to extract
	 * @param defaultVal returned if parameter is missing or not a valid long
	 * @return parameter value as long or defaultVal
	 */
	public static long getLong(HttpServletRequest request, String paramName, long defaultVal) {
		try {
			return Long.parseLong(request.getParameter(paramName));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
	
	/**
	 * @param request object
	 * @return device ("web", "wk", or "feature") or null if it cannot be determined
	 */
	public static String getDevice(HttpServletRequest request) {
		if (request == null) return "wk";
		// parameter takes precedence
		String device = request.getParameter("device"), sessionDevice = (String)request.getSession().getAttribute("device");
		// we don't have parameter so take from session
		if (device == null) device = sessionDevice;
		if (device == null && internalWkCheck(request)) device = "wk";
		if (device != null && !device.equals(sessionDevice)) request.getSession().setAttribute("device",device);
		return device == null ? "wk" : device;
	}
	
	/**
	 * <p>Determines if the request was generated by a webkit device.  This method
	 * checks the user-agent and other session attributes to determine if the
	 * device is a webkit device.  <b>NOTE</b> - if this method is called for the first
	 * time in a session, the result is cached in the session attribute {@code device.webkit}.
	 * Once {@code device.webkit} is set, this value is returned for the duration of the session.</p>
	 *
	 * @param request the HttpServletRequest
	 * @return true if request is from a touchscreen webkit device (Android, iPhone, PalmOS) but not Blackberry
	 */
	public static boolean isWebKitDevice(HttpServletRequest request) {
		return "wk".equals(getDevice(request));
	}
	
	private static boolean internalWkCheck(HttpServletRequest request) {
		if (request == null) {
			return false;
		}
		String device = request.getParameter("device");
		if (device == null) device = (String)request.getSession().getAttribute("device");
		if ("web".equals(device) || "feature".equals(device)) return false;
		
		String deviceWebkitSessionAttr = (String) request.getSession().getAttribute("device.webkit");
		if ("true".equals(deviceWebkitSessionAttr)) {
			return true;
		}
		else if ("false".equals(deviceWebkitSessionAttr)) {
			return false;
		}

		if ("iphone".equals(request.getSession().getAttribute("spoofua")) || "iphone".equals(request.getParameter("spoofua")) || "android".equals(request.getSession().getAttribute("spoofua")) || "android".equals(request.getParameter("spoofua")))
			return true;

		String userAgent = null;
		try { userAgent = request.getHeader("User-Agent"); }
		catch (Throwable t) {}

		// check for webkit
		if (userAgent != null && (userAgent.indexOf("AppleWebKit") > -1 || userAgent.indexOf("CFNetwork") > -1)) {
			request.getSession().setAttribute("device.webkit", "true");
			return true;
		}

		request.getSession().setAttribute("device.webkit", "false");
		return false;
	}
	
	/**
	 * Check UserAgent and return true/false for animated GIF support
	 * Example: Novarra browser (on many US Cel devices) does not have last frame static - continue to cycle..
	 */
	public static boolean hasAnimGifSupport(HttpServletRequest request) {

		String userAgent = null;
		try { userAgent = request.getHeader("User-Agent"); }
		catch (Throwable t) {}

		if (userAgent != null && (userAgent.indexOf("Novarra-Vision") > -1))
				return (false);

		return(true);
		
	}
	/**
	 * Build url with player's access token always included
	 * @param player playing game
	 * @param url to build
	 * @param response to encode properly
	 * @return url
	 */
	public static String buildUrl(Player player, String url, HttpServletResponse response) {
		if (url == null) url = "/";
		if (player != null) {
			url += (url.contains("?") ? "&" : "?") + "accessToken="+player.getAccessToken();
		}
		return response.encodeURL(url);
	}
}
