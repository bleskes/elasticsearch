package com.prelert.web;

import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Spring Controller implementation that mimics standard
 * ServletWrappingController behaviour (see its documentation), but with the
 * important difference that it doesn't instantiate the Servlet instance
 * directly but delegate for this the BeanContext, so that we can also use IoC.
 */
public class ServletWrappingController extends AbstractController 
	implements BeanNameAware, InitializingBean, DisposableBean
{
	private Class<?> m_ServletClass;
	private String m_ServletName;
	private Properties m_InitParameters = new Properties();
	private String m_BeanName;
	private Servlet m_ServletInstance;


	public void setServletClass(Class<?> servletClass)
	{
		m_ServletClass = servletClass;
	}


	public void setServletName(String servletName)
	{
		m_ServletName = servletName;
	}


	public void setInitParameters(Properties initParameters)
	{
		m_InitParameters = initParameters;
	}


	public void setBeanName(String name)
	{
		m_BeanName = name;
	}


	public void setServletInstance(Servlet servletInstance)
	{
		m_ServletInstance = servletInstance;
	}


	public void afterPropertiesSet() throws Exception
	{
		if (m_ServletInstance == null)
		{
			throw new IllegalArgumentException("servletInstance is required");
		}
		if (!Servlet.class.isAssignableFrom(m_ServletInstance.getClass()))
		{
			throw new IllegalArgumentException("servletInstance [" + 
					m_ServletClass.getName() + 
					"] needs to implement interface [javax.servlet.Servlet]");
		}
		if (m_ServletName == null)
		{
			m_ServletName = m_BeanName;
		}
		m_ServletInstance.init(new DelegatingServletConfig());
	}


	protected ModelAndView handleRequestInternal(HttpServletRequest request,
	        HttpServletResponse response) throws Exception
	{
		m_ServletInstance.service(request, response);
		return null;
	}


	public void destroy()
	{
		m_ServletInstance.destroy();
	}

	private class DelegatingServletConfig implements ServletConfig
	{
		public String getServletName()
		{
			return m_ServletName;
		}


		public ServletContext getServletContext()
		{
			return getWebApplicationContext().getServletContext();
		}


		public String getInitParameter(String paramName)
		{
			return m_InitParameters.getProperty(paramName);

		}


		public Enumeration<Object> getInitParameterNames()
		{
			return m_InitParameters.keys();
		}
	}
}
