/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.process;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.prelert.data.ProcessStatus;
import com.prelert.data.ProcessStatus.ProcessRunStatus;
import com.prelert.proxy.runpolicy.RunPolicy;


/**
 * Process monitor class which monitors log4j/log4cxx log messages sent to it
 * by an XmlSocketAppender appender, with XML documents expected to be encoded
 * using UTF-8.
 *
 * Uses java.NIO to do non-blocking reads on multiple connections.
 */
public class ProcessMonitor implements Runnable
{
	/**
	 * This needs to be set to the name of a single byte character set that the
	 * JVM is guaranteed to support.
	 */
	private static final String SINGLE_BYTE_CHARSET = "ISO-8859-1";

	/**
	 * This needs to be set to the name of the character set that log4cxx
	 * uses to send its XML messages over the wire.
	 */
	private static final String LOG4CXX_CHARSET = "UTF-8";

	/**
	 * What string indicates the end of a single XML document?
	 * NB: The code in this class assumes that the encoding of this string will
	 * be the same in both the Latin1 and UTF-8 character sets.  This will only
	 * be the case if this constant contains ASCII characters ONLY.
	 */
	private static final String DOC_END_TOKEN = "</log4j:event>";

	/**
	 * Constants for the required elements of a log event XML document
	 */
	private static final String LOG_EVENT_ROOT = "log4j:event";
	private static final String LOG_EVENT_ATTR_LOGGER = "logger";
	private static final String LOG_EVENT_ATTR_TIMESTAMP = "timestamp";
	private static final String LOG_EVENT_ATTR_LEVEL = "level";
	private static final String LOG_EVENT_ATTR_THREAD = "thread";
	private static final String LOG_EVENT_MESSAGE = "log4j:message";

	/**
	 * It's very bad for the process monitor thread to stop, but obviously if
	 * unexpected exceptions persist we have no choice
	 */
	private static final int MAX_CONSECUTIVE_EXCEPTIONS = 10;

	/**
	 * These constants MUST match EXACTLY the equivalents in CProcess.cc and
	 * CProcess_Windows.cc
	 */
	public static final String PROCESS_STARTING_MSG = "Process Starting.";
	public static final String PROCESS_STARTED_MSG = "Process Started.";
	public static final String PROCESS_SHUTTING_DOWN_MSG = "Process Shutting Down.";
	public static final String PROCESS_EXITING_MSG = "Process Exiting.";


	private static Logger s_Logger = Logger.getLogger(ProcessMonitor.class);

	public static final int DEFAULT_LOG_PORT = org.apache.log4j.net.SocketAppender.DEFAULT_PORT;

	private ServerSocketChannel m_ServerChannel;
	private Selector m_Selector;

	private Thread m_Thread;
	private boolean m_Quit;
	private Map<String, ProcessStatus> m_ProcessStatusByName;

	private boolean m_HasFatalError;
	private String m_FatalErrorMessage;

	/**
	 * Creates the document builders used to parse the incoming XML.
	 */
	private DocumentBuilderFactory m_DocBuilderFactory;


	/**
	 * Creates the monitor object.
	 *
	 * @param port - The log4j SocketAppender port.
	 *               The default value is DEFAULT_LOG_PORT
	 * @throws IOException
	 */
	public ProcessMonitor(int port) throws IOException
	{
		m_Thread = new Thread(this, "ProcessMonitor");
		m_Quit = false;

		m_ProcessStatusByName = new HashMap<String, ProcessStatus>();

		m_Selector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		m_ServerChannel = ServerSocketChannel.open();
		m_ServerChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port.
		// NB: hardcoding localhost here means the C++ processes must be running
		// on the same machine as the proxy.
		// TODO - if there is ever a requirement to have the proxy listen on all
		// interfaces of the machine, so that the C++ processes can run on a
		// different machine, use the InetSocketAddress constructor that ONLY
		// takes a port, i.e. delete "localhost" from the following line.  This
		// would obviously create the risk of rogue processes on other machines
		// sending corrupt data to the port.
		InetSocketAddress isa = new InetSocketAddress("localhost", port);
		m_ServerChannel.socket().bind(isa);

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		m_ServerChannel.register(m_Selector, SelectionKey.OP_ACCEPT);

		m_HasFatalError = false;
		m_FatalErrorMessage = "";

		m_DocBuilderFactory = DocumentBuilderFactory.newInstance();
	}


	/**
	 * Waits for a socket connection then processes any loggingEvents
	 * sent to the server.
	 */
	@Override
	public void run()
	{
		s_Logger.info("ProcessMonitor thread starting");

		try
		{
			int consecutiveExceptions = 0;

			while (!m_Quit)
			{
				try
				{
					// Wait for an event one of the registered channels
					m_Selector.select();

					// Iterate over the set of keys for which events are available
					Iterator<SelectionKey> selectedKeys = m_Selector.selectedKeys().iterator();
					while (selectedKeys.hasNext())
					{
						SelectionKey key = selectedKeys.next();
						selectedKeys.remove();

						if (!key.isValid())
						{
							continue;
						}

						if (key.isAcceptable())
						{
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

							// Accept the connection and make it non-blocking
							SocketChannel socketChannel = serverSocketChannel.accept();
							if (socketChannel == null)
							{
								continue;
							}

							socketChannel.configureBlocking(false);

							// Register the new SocketChannel with our Selector, indicating
							// we'd like to be notified when there's data waiting to be read
							socketChannel.register(this.m_Selector, SelectionKey.OP_READ);
						}
						else if (key.isReadable())
						{
							readLoggingEvent(key);
						}
					}

					consecutiveExceptions = 0;
				}
				catch (IOException e)
				{
					s_Logger.info("Caught java.io.IOException: " + e);
				}
				catch (ClosedSelectorException e)
				{
					if (!m_Quit) // only log error if not quitting.
					{
						s_Logger.info("ProcessMonitor run()", e);
					}
				}
				catch (Exception e)
				{
					s_Logger.error("Unexpected exception in ProcessMonitor thread", e);
					++consecutiveExceptions;
					if (consecutiveExceptions > MAX_CONSECUTIVE_EXCEPTIONS)
					{
						s_Logger.error("Too many consecutive exceptions in ProcessMonitor thread "
										+ consecutiveExceptions);
						break;
					}
				}
			}
		}
		finally
		{
			// Try to log this even if we're exiting due to a serious error,
			// because if this thread exits prematurely it can deadlock a
			// process that's logging at that moment, so at least we'll know
			// there's a problem
			s_Logger.info("ProcessMonitor thread ending");
		}
	}


	/**
	 * Non-blocking read of logging events from the selection key.
	 *
	 * Logging events are sent as XML in the UTF-8 character set.  As partial
	 * XML documents may be read from the socket, a buffer is used.  It is
	 * attached to the key and exists for the life of the key.  Complete XML
	 * documents are deleted from the buffer once they are read, and partial
	 * XML documents are retained to be added to once more data is read from the
	 * socket.
	 *
	 * @param key
	 * @throws IOException
	 */
	private void readLoggingEvent(SelectionKey key) throws IOException
	{
		// Set this reasonably large in case the log message contains a long
		// string.  Log messages that don't fit in this buffer will end up
		// being discarded, and may also cause the following message to be
		// discarded too.
		final int BUFFER_SIZE = 4096;

		SocketChannel socketChannel = (SocketChannel)key.channel();

		ByteBuffer byteBuffer = (ByteBuffer)key.attachment();
		if (byteBuffer == null)
		{
			byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

			key.attach(byteBuffer);
		}

		int numRead;
		try
		{
			numRead = socketChannel.read(byteBuffer);
		}
		catch (IOException e)
		{
			s_Logger.error("Problem reading from socket: " + e);
			byteBuffer.clear();
			return;
		}

		// If numRead is negative, it means the socket has been disconnected.
		if (numRead < 0)
		{
			try
			{
				socketChannel.close();
			}
			finally
			{
				key.cancel();
			}
			return;
		}

		if (numRead == 0)
		{
			return;
		}

		if (!byteBuffer.hasArray())
		{
			s_Logger.error("Inconsistency - " + numRead +
					" bytes read from socket but byte buffer has no array");
			return;
		}

		int remainingBytes = byteBuffer.position() - byteBuffer.arrayOffset();

		// For dividing the input into separate XML documents, convert it to a
		// string using a single byte character set.  That way the positions in
		// the string will map to the same positions in the byte buffer.  This
		// works because the encoding of the document end token we're looking
		// for is the same in Latin1 and UTF-8 (because it's pure ASCII).  The
		// message itself may be misinterpreted at this stage, if it contains
		// characters with codes higher than 127.
		String latin1Str;
		try
		{
			latin1Str = new String(byteBuffer.array(),
									byteBuffer.arrayOffset(),
									remainingBytes,
									SINGLE_BYTE_CHARSET);
		}
		catch (UnsupportedEncodingException e)
		{
			// This should never happen according to the JVM specification
			s_Logger.error(SINGLE_BYTE_CHARSET +
							" character set not supported");
			return;
		}

		// This variable holds the position in the buffer up to which we've
		// successfully read complete Java objects
		int bytesSuccessfullyDecoded = 0;

		int docEndTokenStart = latin1Str.indexOf(DOC_END_TOKEN);
		while (docEndTokenStart >= 0)
		{
			int docEnd = docEndTokenStart + DOC_END_TOKEN.length();
			int docStart = bytesSuccessfullyDecoded;
			int docSize = docEnd - docStart;

			try
			{
				// The XML is really in UTF-8, so now we know the bounds of a
				// single document, reinterpret the bytes that make up that
				// document as UTF-8.
				String utf8Str = new String(byteBuffer.array(),
											byteBuffer.arrayOffset() + docStart,
											docSize,
											LOG4CXX_CHARSET);
				parseEvent(utf8Str);

				// Even if there's a problem PARSING the XML, we successfully
				// DECODED the bytes, so still increment this counter
				bytesSuccessfullyDecoded += docSize;
				remainingBytes -= docSize;
			}
			catch (UnsupportedEncodingException e)
			{
				// As long as log4cxx's wire character set is UTF-8, this should
				// never happen according to the JVM specification
				s_Logger.error(LOG4CXX_CHARSET +
								" character set not supported");
				return;
			}

			docEndTokenStart = latin1Str.indexOf(DOC_END_TOKEN,
												bytesSuccessfullyDecoded);
		}

		// Was there a partial object at the end of the buffer?
		if (remainingBytes > 0 && remainingBytes < BUFFER_SIZE)
		{
			// Shuffle up the remaining data to the beginning of the buffer
			byteBuffer.position(0);
			for (int i = bytesSuccessfullyDecoded; i < bytesSuccessfullyDecoded + remainingBytes; ++i)
			{
				byteBuffer.put(byteBuffer.get(i));
			}
		}
		else
		{
			// Either:
			// 1) We read everything in the buffer
			// 2) We didn't read anything from the buffer, but it's full - this
			//    case could occur if there was a really long log message of if
			//    someone is sending junk to our port

			if (remainingBytes >= BUFFER_SIZE)
			{
				s_Logger.warn("Valid log XML not found in " + remainingBytes +
							" bytes of socket input - discarding");
			}

			// Completely empty the buffer
			byteBuffer.clear();
		}
	}


	/**
	 * Parse the logging event and set the process status accordingly.
	 *
	 * In the case of a fatal error, the message is saved, and, in the download
	 * product, all the Prelert processes are stopped.
	 *
	 * @param eventXmlDoc A <code>String</code> containing the XML document to
	 *                    be parsed
	 * @return true if the event is successfully parsed, otherwise false.
	 */
	private boolean parseEvent(String eventXmlDoc)
	{
		if (eventXmlDoc == null)
		{
			s_Logger.error("Attempt to parse null XML document");
			return false;
		}

		try
		{
			DocumentBuilder docBuilder = m_DocBuilderFactory.newDocumentBuilder();
			StringReader strReader = new StringReader(eventXmlDoc);
			Document doc;
			try
			{
				doc = docBuilder.parse(new InputSource(strReader));
			}
			finally
			{
				strReader.close();
			}

			// First attempt to get the root node, which should be called
			// log4j:event
			Element rootNode = doc.getDocumentElement();
			if (rootNode == null || !LOG_EVENT_ROOT.equals(rootNode.getNodeName()))
			{
				s_Logger.warn("XML document did not have root node " +
							LOG_EVENT_ROOT + " : " + eventXmlDoc);
				return false;
			}

			Attr loggerAttr = rootNode.getAttributeNode(LOG_EVENT_ATTR_LOGGER);
			if (loggerAttr == null)
			{
				s_Logger.warn("XML document root " + LOG_EVENT_ROOT +
							" did not contain " + LOG_EVENT_ATTR_LOGGER +
							" attribute : " + eventXmlDoc);
				return false;
			}

			// The C++ processes store their log ID in the logger field.  This
			// class makes the assumption that the log ID will match the process
			// name used with the prelert_ctl script.
			String processName = loggerAttr.getValue();

			Attr timestampAttr = rootNode.getAttributeNode(LOG_EVENT_ATTR_TIMESTAMP);
			if (timestampAttr == null)
			{
				s_Logger.warn("XML document root " + LOG_EVENT_ROOT +
							" did not contain " + LOG_EVENT_ATTR_TIMESTAMP +
							" attribute : " + eventXmlDoc);
				return false;
			}

			long timestamp;
			try
			{
				timestamp = Long.parseLong(timestampAttr.getValue());
			}
			catch (NumberFormatException e)
			{
				s_Logger.warn("XML document root " + LOG_EVENT_ROOT +
							' ' + LOG_EVENT_ATTR_TIMESTAMP +
							" attribute could not be converted to a timestamp : " +
							timestampAttr.getValue());
				return false;
			}

			Attr levelAttr = rootNode.getAttributeNode(LOG_EVENT_ATTR_LEVEL);
			if (levelAttr == null)
			{
				s_Logger.warn("XML document root " + LOG_EVENT_ROOT +
							" did not contain " + LOG_EVENT_ATTR_LEVEL +
							" attribute : " + eventXmlDoc);
				return false;
			}
			String errorLevel = levelAttr.getValue();

			Attr threadAttr = rootNode.getAttributeNode(LOG_EVENT_ATTR_THREAD);
			if (threadAttr == null)
			{
				s_Logger.warn("XML document root " + LOG_EVENT_ROOT +
							" did not contain " + LOG_EVENT_ATTR_THREAD +
							" attribute : " + eventXmlDoc);
				return false;
			}

			NodeList nodeList = rootNode.getElementsByTagName(LOG_EVENT_MESSAGE);
			if (nodeList == null || nodeList.getLength() == 0)
			{
				s_Logger.warn("XML document did not contain node " +
							LOG_EVENT_MESSAGE +
							" : " + eventXmlDoc);
				return false;
			}

			Node messageNode = nodeList.item(0);

			// Use getTextContent(), so it works regardless of whether the
			// message is in a <![CDATA[ ]]>
			String message = messageNode.getTextContent();

			// Get the previous status
			ProcessStatus status;
			synchronized (m_ProcessStatusByName)
			{
				status = m_ProcessStatusByName.get(processName);

				if (status == null)
				{
					status = new ProcessStatus();
					status.setProcessName(processName);
					m_ProcessStatusByName.put(processName, status);
				}
			}
			ProcessRunStatus previousStatus = status.getStatus();

			if (errorLevel.equals(Level.FATAL.toString()))
			{
				handleFatalError(message);
				status.setStatus(ProcessRunStatus.FATAL_ERROR);
			}
			else if (errorLevel.equals(Level.ERROR.toString()))
			{
				status.setStatus(ProcessRunStatus.ERROR);
			}

			if (message.equals(PROCESS_STARTING_MSG))
			{
				status.setStatus(ProcessRunStatus.STARTING);
			}
			else if (message.equals(PROCESS_STARTED_MSG))
			{
				status.setStatus(ProcessRunStatus.RUNNING);
			}
			else if (message.equals(PROCESS_SHUTTING_DOWN_MSG))
			{
				status.setStatus(ProcessRunStatus.STOPPING);
			}
			else if (message.equals(PROCESS_EXITING_MSG))
			{
				status.setStatus(ProcessRunStatus.STOPPED);
			}

			status.setMessage(message);
			status.setTimeStamp(new Date(timestamp));

			ProcessRunStatus currentStatus = status.getStatus();

			// Only log the message at a level that's likely to get printed if
			// the status has changed.  Otherwise the proxy log ends up
			// containing all the back end logs too.
			if (currentStatus == previousStatus)
			{
				s_Logger.trace(status);
			}
			else
			{
				s_Logger.info(status);
			}

			// Successfully parsed the event
			return true;
		}
		catch (ParserConfigurationException e)
		{
			s_Logger.error("Could not parse XML document : " +
							eventXmlDoc);
			s_Logger.error(e);
		}
		catch (SAXException e)
		{
			s_Logger.error("Could not parse XML document : " +
							eventXmlDoc);
			s_Logger.error(e);
		}
		catch (IOException e)
		{
			s_Logger.error("Could not parse XML document : " +
							eventXmlDoc);
			s_Logger.error(e);
		}

		// We only get here if there was an exception whilst parsing the
		// document
		return false;
	}


	/**
	 * Start the thread.
	 */
	public void start()
	{
		m_Thread.start();
	}


	/**
	 * Ask the thread to exit at the next opportunity.
	 */
	public void quit()
	{
		m_Quit = true;
		s_Logger.info("ProcessMonitor thread instructed to quit");
		try
		{
			m_Selector.close();
		}
		catch (IOException e)
		{
			s_Logger.error("IOException thrown when quitting: " + e);
		}
	}


 	/**
	 * Records the message and, in the download product, shuts down the Prelert
	 * processes.  (In the real-time product, we let the back end processes try
	 * to restart and eventually recover.)
	 *
	 * Records details of the fatal error message.  Only first fatal error is
	 * recorded, subsequent fatal error messages are ignored.
	 *
	 * @param message The text of the fatal log message.
	 */
	private void handleFatalError(String message)
	{
		if (m_HasFatalError == false)
		{
			// In the download product ONLY, stop all the back end processes.
			// In the real-time product, it's better to let the back end
			// processes be restarted by daemontools and hope they eventually
			// get over the cause of the fatal error.  The real-time product may
			// need to run unattended for many months, and if we shut it down
			// nobody is going to manually restart it.
			if (RunPolicy.isDownloadProduct())
			{
				ProcessManager.stopProcesses();
			}

			m_HasFatalError = true;
			m_FatalErrorMessage = message;
		}
	}


	/**
	 * Return the status of the process.
	 * If the process name isn't recognised then UNKNOWN
	 * status is returned.
	 *
	 * @param processName The name of the process (as specified by the --logid
	 *                    argument of the C++ processes).  Usually this will be
	 *                    a shortened version of the program name.  The proxy
	 *                    assumes that the log ID will match the process name
	 *                    used by the prelert_ctl script.
	 * @return
	 */
	public ProcessStatus getProcessStatus(String processName)
	{
		ProcessStatus status;
		synchronized (m_ProcessStatusByName)
		{
			status = m_ProcessStatusByName.get(processName);
		}

		if (status == null)
		{
			status = new ProcessStatus();
			status.setProcessName(processName);
		}

		return status;
	}


	/**
	 * Returns true if any of the processes have had a fatal error.
	 * @return
	 */
	public boolean hasFatalError()
	{
		return m_HasFatalError;
	}


	/**
	 * If a fatal error has occurred this returns the error message.
	 *
	 * @return
	 */
	public String getFatalErrorMessage()
	{
		return m_FatalErrorMessage;
	}


	/**
	 * Entry point for standalone testing.
	 *
	 * @param args Not used.
	 */
	public static void main(String args[])
	{
		// Configure logging
		BasicConfigurator.configure();

		// Hardcode the level to TRACE for standalone testing
		s_Logger.setLevel(Level.TRACE);

		// Log the copyright and version
		s_Logger.info("Start standalone logger testing");

		try
		{
			ProcessMonitor processMonitor = new ProcessMonitor(DEFAULT_LOG_PORT);

			// The application never exits - kill it manually after testing
			// is complete
			processMonitor.start();
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}
	}

}
