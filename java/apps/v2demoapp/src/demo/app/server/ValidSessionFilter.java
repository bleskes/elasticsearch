package demo.app.server;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import demo.app.data.InvalidSessionException;

public class ValidSessionFilter implements Filter
{

	static Logger logger = Logger.getLogger(ValidSessionFilter.class);
	private FilterConfig m_FilterConfig;
	
	public void init(FilterConfig filterConfig) throws ServletException
	{
		logger.debug("ValidSessionFilter.init()");
		m_FilterConfig = filterConfig;

	}
	
	
	@Override
	public void destroy()
	{


	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
	        FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpSession session = httpRequest.getSession(false);
		
		if (session != null)
		{
			logger.debug("ValidSessionFilter.doFilter() session id=" + session.getId());
		}
		else
		{
			logger.debug("ValidSessionFilter.doFilter() session is null");
		}
		
		if (session != null)
		{
			chain.doFilter(request, response);
		}
		else
		{
			((HttpServletResponse) response).sendRedirect("/desktopapp/DesktopApp.html");
			
			
			
			//ServletContext context = m_FilterConfig.getServletContext();
			//RequestDispatcher dispatcher = context.getRequestDispatcher("/DesktopApp.html");
			//dispatcher.forward(request, response);
			//return;
		}
	}




}
