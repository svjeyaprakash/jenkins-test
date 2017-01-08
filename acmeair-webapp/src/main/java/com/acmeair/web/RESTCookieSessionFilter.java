/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.web;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.acmeair.entities.CustomerSession;
import com.acmeair.service.CustomerService;

public class RESTCookieSessionFilter implements Filter {
	
	static final String LOGIN_USER = "acmeair.login_user";
	private static final String LOGIN_PATH = "/rest/api/login";
	private static final String LOGOUT_PATH = "/rest/api/login/logout";
	
	private CustomerService customerService = ServiceLocator.getService(CustomerService.class);
	
	@Resource (name="jdbc/acmeairdatasource")
	DataSource source1;
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,	FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)resp;
		
		String path = request.getServletPath() + request.getPathInfo();
		
		// could do .startsWith for now, but plan to move LOGOUT to its own REST interface eventually
		if (path.endsWith(LOGIN_PATH) || path.endsWith(LOGOUT_PATH) || path.startsWith("/rest/api/loader/")) {
			// if logging in, let the request flow
			chain.doFilter(req, resp);
			return;
		}
		
		Cookie cookies[] = request.getCookies();
		Cookie sessionCookie = null;
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(LoginREST.SESSIONID_COOKIE_NAME)) {
					sessionCookie = c;
				}
				if (sessionCookie!=null)
					break; 
			}
			String sessionId = "";
			if (sessionCookie!=null) // We need both cookie to work
				sessionId= sessionCookie.getValue().trim();
			// did this check as the logout currently sets the cookie value to "" instead of aging it out
			// see comment in LogingREST.java
			if (sessionId.equals("")) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			// Need the URLDecoder so that I can get @ not %40
			CustomerSession cs = customerService.validateSession(sessionId);
			if (cs != null) {
				request.setAttribute(LOGIN_USER, cs.getCustomerid());
				chain.doFilter(req, resp);
				return;
			}
			else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		
		// if we got here, we didn't detect the session cookie, so we need to return 404
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}
}
