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

package com.prelert.proxy.inputmanager;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.prelert.proxy.inputmanager.DataCollectionMode;


/**
 * This class encapsulates a TCP connection to one of the Prelert backend
 * processes, which act as TCP servers implemented by libPreIpc.  This class is
 * NOT suitable for communicating with third party systems via TCP, because it
 * incorporates some implementation specific knowledge about how libPreIpc
 * works.  In particular:
 * <ol>
 * <li>All communications with the Prelert back end must use the UTF-8 character
 *    set.</li>
 * <li>The TCP stream is divided into messages by terminating each message with
 *    a single zero byte.</li>
 * <li>In client-server connections using the Prelert framework, the server may
 *    exert back-pressure on the client, i.e. tell it to send data more slowly
 *    to avoid overwhelming the server.</li>
 * <li> If the socket connection fails, we'll try to reconnect several times for
 *    each message subsequently sent, but after these retries we'll drop the
 *    message</li>
 *</ol>
 * ANY CHANGES TO THIS CLASS NEED TO BE MADE WITH CONSIDERATION OF THE IMPACT ON
 * THE CORRESPONDING C++ CODE.  In particular, see the files in the
 * <code>$PRELERT_SRC_HOME/lib/ipc</code> directory.
 * </br>
 * The {@link #queueMessage()} function performs non-blocking I/O setting up the message
 * to be sent by another thread. However, if the number of queued messages is
 * greater than MAX_QUEUED_MGS then {@link #queueMessage()} will block until one of those
 * messages have been sent.
 * </br>
 */
public class PrelertBackendTCPClient extends Thread
{
	static Logger s_Logger = Logger.getLogger(PrelertBackendTCPClient.class);

	/**
	 * Where is the server that we're going to connect to?
	 */
	private String m_Host;
	private int m_Port;

	/**
	 * Are we going to take any notice of instructions from the server to slow
	 * down?
	 */
	private boolean m_AcceptingBackPressure;

	/**
	 * Commands longer than this number of BYTES (not characters) will not
	 * be interpreted
	 */
	private static final int MAX_COMMAND_LEN = 128;

	/**
	 * The socket channel we'll use to connect to the Prelert backend.
	 */
	volatile private SocketChannel m_SocketChannel;

	/**
	 * Buffered output stream for sending messages.
	 * A outputstream can only be used with a channel when it is
	 * in non-blocking mode.
	 */
	volatile private BufferedOutputStream m_BufferedOutput;

	/**
	 * The buffer to hold slow down/speed up commands
	 * from the backend.
	 */
	private ByteBuffer m_CommandBuffer;


	/**
	 * A Blocking queue to hold the messages.
	 */
	private BlockingQueue<String> m_MessageQueue;

	/**
	 * Input managers tend to send data in big blocks, so in the realtime
	 * product the queue needs to be able to accept a large amount of messages
	 * before it blocks the input manager's thread because we want to maximise
	 * throughput of the input managers.
	 */
	static final private int MAX_QUEUED_MGS_REALTIME = 1000000;

	/**
	 * In contrast to the realtime product, when running in CAV mode, if the
	 * back-end cannot keep up, we want to pull data more slowly from the third
	 * party system, so set the queue size small so that it blocks the input
	 * managers more readily.
	 */
	static final private int MAX_QUEUED_MGS_CAV = 1000;

	/**
	 * By how much has the server requested that we slow down between messages?
	 */
	volatile private long m_SlowDownTimeMS;

	/**
	 * Stop flag for the thread.
	 */
	volatile private boolean m_Quit;

	/**
	 * Flag to indicate that we're waiting for the queue to empty.
	 */
	volatile private boolean m_WaitingForEmptyQueue;
	
	/**
	 * Flag to indicate a message is currently being sent.
	 * This may be true even if the message queue is empty.
	 */
	volatile private boolean m_isSendingMsg;

	/**
	 * These commands sent through the TCP stream control back-pressure, i.e.
	 * instructions to slow down or speed up the message sending rate.  These
	 * strings MUST EXACTLY MATCH those defined in <code>CRecvBuffer.cc</code>.
	 */
	public static final String CMD_ACCEPT_BACK_PRESSURE = "PRELERT_CMD_ABP";
	public static final String CMD_SLOW_DOWN = "PRELERT_CMD_SD";
	public static final String CMD_SPEED_UP = "PRELERT_CMD_SU";

	/**
	 * The character set used for communications by the Prelert backend.
	 */
	public static final String BACKEND_CHARSET = "UTF-8";

	/**
	 * The code of the character used by the Prelert backend to terminate
	 * messages.
	 */
	public static final int BACKEND_MSG_TERMINATOR = 0;

	/**
	 * If message sending fails, how many times should we retry before giving
	 * up.
	 */
	public static final int RETRIES = 3;
	public static final long RETRY_DELAY_MS = 5000;

	/**
	 * If a slow down command is received, but it's not possible to determine
	 * a specific slow down, what delay should be used?
	 */
	private static final long DEFAULT_PROCESSING_DELAY_MS = 100;


	/**
	 * Create a <code>PrelertBackendTCPClient</code>.
	 *
	 * @param host The name or IP address of the host to connect to
	 * @param port The TCP port number to connect to
	 * @param acceptBackPressure Will we take any notice of instructions from
	 *                           the server to slow down?
	 * @param collectionMode The mode the parent Input manager is running in.
	 * 						 This parameter determines the size of the message
	 * 						 that is created.
	 */
	public PrelertBackendTCPClient(String host,
									int port,
									boolean acceptBackPressure,
									DataCollectionMode collectionMode)
	{
		super("PrelertBackendTCPClient");

		m_Host = host;
		m_Port = port;

		m_Quit = false;
		m_WaitingForEmptyQueue = false;
		m_isSendingMsg = false;

		m_AcceptingBackPressure = acceptBackPressure;
		m_SlowDownTimeMS = 0;

		if (collectionMode == DataCollectionMode.HISTORICAL)
		{
			m_MessageQueue = new LinkedBlockingQueue<String>(MAX_QUEUED_MGS_CAV);
		}
		else
		{
			m_MessageQueue = new LinkedBlockingQueue<String>(MAX_QUEUED_MGS_REALTIME);
		}

		s_Logger.info("Blocking queue capacity in Prelert TCP client set to " +
						m_MessageQueue.remainingCapacity());

		m_CommandBuffer = ByteBuffer.allocate(MAX_COMMAND_LEN);

		// Try to open the connection immediately, so that the user doesn't have
		// to wait minutes for typos in the host/port to be reported in the log
		openConnection();
	}


	/**
	 * The <code>closeConnection()</code> method should have been called when
	 * the client was instructed to quit.  However, just in case this wasn't
	 * done, we make sure the socket is closed during garbage collection to
	 * prevent a resource leak.
	 */
	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			if (closeConnection() == true)
			{
				s_Logger.error("Prelert TCP client was garbage collected with socket still open");
			}
		}
		finally
		{
			super.finalize();
		}
	}


	/**
	 * This function blocks waiting for the a message to be added
	 * to the message queue. When a message is added it is sent over
	 * the TCP socket.After each message is sent a non-blocking read
	 * is made to check for feedback from the backend client.
	 * This continues until quit() is called.
	 */
	@Override
	public void run()
	{
		while (!m_Quit)
		{
			try
			{
				String msg = m_MessageQueue.poll(1000, TimeUnit.MILLISECONDS);
				if (msg != null)
				{
					sendMessage(msg, m_MessageQueue.isEmpty());
				}

				readFeedback();
			}
			catch (InterruptedException e)
			{
				s_Logger.info("Interrupted whilst polling the message queue.");
			}
			catch (IOException e)
			{
				s_Logger.info("IOException sending message : " + e);
			}
		}

		closeConnection();

		if (m_MessageQueue.size() > 0)
		{
			s_Logger.warn("Prelert TCP client run loop ending with " +
						m_MessageQueue.size() +
						" unsent messages intended for server on host " +
						m_Host + ", port " + m_Port);
		}
	}


	/**
	 * Read any feedback from the connected socket channel.  When a terminator
	 * is seen send the message to be decoded.
	 *
	 * The read is done as a non-blocking IO call as it is likely that there
	 * will not be any data to read.  The <code>SocketChannel</code> is put into
	 * non-blocking mode before the read.  Other code that wants it in blocking
	 * mode is responsible for resetting this.
	 */
	private void readFeedback()
	{
		try
		{
			// Return if not connected - sendMessage() is responsible for
			// reconnections.
			if (m_SocketChannel == null || !m_SocketChannel.isConnected())
			{
				return;
			}

			m_SocketChannel.configureBlocking(false);

			// Non-blocking read.
			int bytesRead = m_SocketChannel.read(m_CommandBuffer);

			// -1 means the channel is at end of stream.
			if (bytesRead < 0)
			{
				// Don't log error if caused by stopping.
				if (!m_Quit)
				{
					s_Logger.info("Server on host " + m_Host + ", port " + m_Port +
									" has closed the TCP connection");
				}

				closeConnection();
				return;
			}

			if (bytesRead == 0)
			{
				return;
			}

			if (!m_CommandBuffer.hasArray())
			{
				s_Logger.error("Inconsistency - " + bytesRead +
							" bytes read from socket but byte buffer has no array");
				return;
			}

			int cmdStart = 0;
			int cmdEnd = -1;

			byte buffer[] = m_CommandBuffer.array();
			for (int i = 0; i < m_CommandBuffer.position(); ++i)
			{
				if (m_CommandBuffer.get(i) == BACKEND_MSG_TERMINATOR)
				{
					cmdEnd = i;
					decodeCommand(buffer,
								cmdStart + m_CommandBuffer.arrayOffset(),
								cmdEnd + m_CommandBuffer.arrayOffset());
					cmdStart = i + 1;
				}
			}

			// If the buffer is full, but we haven't found a command, that means
			// we're being sent junk, so discard it.  If we have processed all
			// the commands in the buffer then clear it, else shuffle up the
			// unprocessed data to the front of the buffer.
			if (cmdEnd == -1)
			{
				// We didn't find a single command - if the buffer isn't full,
				// we just need to wait until more data is received, but if it
				// IS full, we're being sent junk
				if (m_CommandBuffer.position() == MAX_COMMAND_LEN)
				{
					s_Logger.warn("Valid command not found in " +
								MAX_COMMAND_LEN +
								" bytes of socket input - discarding");
					m_CommandBuffer.clear();
				}
			}
			else if (cmdEnd == m_CommandBuffer.position() - 1)
			{
				// The commands we read from the buffer are exactly the contents
				// of the buffer, so we can chuck the whole thing
				m_CommandBuffer.clear();
			}
			else
			{
				// We read part, but not all of the contents of the buffer
				int remainingBytes = m_CommandBuffer.position() - cmdStart;

				// Shuffle up the remaining data to the beginning of the buffer
				m_CommandBuffer.position(0);
				for (int i = cmdStart; i < cmdStart + remainingBytes; ++i)
				{
					m_CommandBuffer.put(m_CommandBuffer.get(i));
				}
			}
		}
		catch (IOException e)
		{
			// Don't log error if caused by stopping.
			if (!m_Quit)
			{
				s_Logger.info("Exception reading TCP feedback from server on host " +
								m_Host + ", port " + m_Port + " : " + e);
			}

			closeConnection();
		}
	}


	/**
	 * Decode the message that's been received, and, if it's understood
	 * modify the settings of the outer object.
	 *
	 * @param messageLength Length (in bytes) of the message to be decoded
	 */
	private void decodeCommand(byte [] cmdBuffer, int start, int end)
	{
		if (start < 0 || end <= start)
		{
			return;
		}

		int commandLength = end - start;

		// Convert the bytes from UTF-8 to Java's character set
		String command;
		try
		{
			command = new String(cmdBuffer,
								start,
								commandLength,
								BACKEND_CHARSET);
		}
		catch (UnsupportedEncodingException e)
		{
			s_Logger.error("Command with " +
					commandLength + " bytes was not valid " +
					BACKEND_CHARSET + " : " + e);

			// This will cascade through to the unknown command functionality
			// below
			command = "invalid " + BACKEND_CHARSET + " data";
		}

		if (command.startsWith(CMD_SLOW_DOWN))
		{
			// Ignore this if we're not accepting back pressure
			if (m_AcceptingBackPressure)
			{
				// The slow down command may or may not be followed by a number
				// indicating how much to slow down.  If it's not, use a
				// default.
				long slowDownMS = DEFAULT_PROCESSING_DELAY_MS;
				if (command.length() > CMD_SLOW_DOWN.length())
				{
					try
					{
						slowDownMS = Long.parseLong(
								command.substring(CMD_SLOW_DOWN.length()));
					}
					catch (NumberFormatException e)
					{
						s_Logger.error("Cannot decode slowdown milliseconds from "
													+ command);
					}
				}

				if (m_SlowDownTimeMS != slowDownMS)
				{
					s_Logger.debug("Back pressure of " +
									slowDownMS + "ms requested");
				}

				m_SlowDownTimeMS = slowDownMS;
			}
		}
		else
		{
			// The only two commands are slow down and speed up, so if we're in
			// this else block the command should be a speed up.  If it's not,
			// it means either we're being sent junk, or some sort of data
			// corruption has occurred.  Unfortunately, corruption of a speed up
			// message is very bad, as it means the client will be permanently
			// slowed down when the server thinks it's running at full throttle.
			// Therefore, unknown commands are treated as speed up commands.
			// If we speed up when we shouldn't, it's not a disaster, as the
			// server will then ask us to slow down even more.  But if the
			// server asks us to speed up and we don't then the whole system
			// will be slowed down until the next restart.
			if (m_AcceptingBackPressure && !command.equals(CMD_SPEED_UP))
			{
				s_Logger.error("Return channel command not understood : '" +
								command + "' - for safety it will be treated " +
								"as a back pressure cancellation request");
			}

			if (m_AcceptingBackPressure && m_SlowDownTimeMS > 0)
			{
				s_Logger.debug("Back pressure request cancelled");
			}
			m_SlowDownTimeMS = 0;
		}
	}


	/**
	 * Blocks until all the messages currently in the message queue have been
	 * sent or lost.  Messages will be lost if the connection to the server
	 * drops whilst this method is waiting.  This is to avoid the process
	 * hanging indefinitely on system shutdown. If the message queue is empty
	 * this function may still wait on a message being sent. 
	 *
	 * If messages continue to be queued this method may never return.
	 *
	 * Does not guarantee that no further messages will be queued after it has
	 * returned.
	 */
	public void waitUntilAllMessagesSent()
	{
		if (m_MessageQueue.isEmpty() && m_isSendingMsg == false)
		{
			s_Logger.info("No TCP client messages are waiting to be sent to host "
							+ m_Host + ", port " + m_Port);
			return;
		}

		s_Logger.info("Waiting for all TCP client messages to be sent to host "
						+ m_Host + ", port " + m_Port);

		m_WaitingForEmptyQueue = true;

		int remaining = m_MessageQueue.size();
		while (remaining > 0)
		{
			try
			{
				// Sleep for a shorter time if the queue is nearly empty
				Thread.sleep((remaining < 1000) ? 100 : 1000);
			}
			catch (InterruptedException e)
			{
			}

			remaining = m_MessageQueue.size();
		}

		m_WaitingForEmptyQueue = false;

		s_Logger.info("Queue of messages to be sent to host "
						+ m_Host + ", port " + m_Port
						+ " by the TCP client is now empty");
		
		
		// Now wait for messages to be sent. 
		// This should happen quite quickly and ensures
		// all messages are sent before stopping the client.
		while (m_isSendingMsg)
		{
			try
			{
				// Sleep for a shorter time if the queue is nearly empty
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
			}
		}
		
		
	}


	/**
	 * Sets the quit flag and interrupts the thread
	 * then waits for it to join.
	 */
	public void quit()
	{
		s_Logger.info("Stopping TCP client thread");

		m_Quit = true;
		interrupt();
		try
		{
			join();
		}
		catch (InterruptedException e)
		{
		}

		s_Logger.info("TCP client thread terminated");
	}


	/**
	 * Queues the message to be sent via the TCP client.
	 * This function will block if there is no room in the message
	 * queue but it does not block while the message is being sent.
	 * The message queue capacity is set to MAX_QUEUED_MGS.
	 * @param message
	 */
	public void queueMessage(String message)
	{
		try
		{
			m_MessageQueue.put(message);
		}
		catch (InterruptedException e)
		{
			s_Logger.info("Interrupted while putting a message into the message queue.");
		}
	}


	/**
	 * Report on and handle any backlog of messages currently in the queue.
	 * Input managers should call this method at the beginning of each batch of
	 * data they want to send to the back-end.  It enables a sensible response
	 * to the situation where the input managers are churning out data faster
	 * than the back-end processes can consume it.
	 *
	 * When running in real-time mode, the backlog is discarded each time a new
	 * batch of data is ready.  If this wasn't done, messages would build up
	 * over time and the JVM would eventually run out of memory.
	 *
	 * When running in historical mode the messages will never be discarded.
	 * Eventually the message buffer will fill up and slow down the input
	 * manager by blocking its attempts to queue messages.  This in turn will
	 * prevent excessive memory consumption.  Unlike the real-time case,
	 * where the system cannot tolerate falling behind, when pulling historical
	 * data it is acceptable to pull it more slowly, to match the throughput of
	 * the slowest process in the processing chain.
	 *
	 * @param collectionMode The data collection mode the input manager is
	 *                       running in.
	 */
	public void backlogHandler(DataCollectionMode collectionMode)
	{
		// Note: the size of the message queue could be different at each line
		// of this method, because other threads could alter the message queue
		// while it's running.  This isn't likely to be a major problem,
		// although it could make the warning message slightly inaccurate.
		if (m_MessageQueue.size() > 0)
		{
			// Don't discard messages in historical mode - just log a message
			if (collectionMode == DataCollectionMode.HISTORICAL)
			{
				s_Logger.warn("Prelert server process on port " + m_Port +
							" is not keeping up with the data rate - backlog " +
							m_MessageQueue.size() + " messages");
			}
			else
			{
				s_Logger.warn("Prelert server process on port " + m_Port +
							" is not keeping up with the data rate - discarding " +
							m_MessageQueue.size() + " messages");
				m_MessageQueue.clear();
			}
		}
	}


	/**
	 * Send a message via a TCP connection to the server.  If there is
	 * an error, retry multiple times before giving up.
	 *
	 * Messages are terminated with a single 0 byte.  This is what the Prelert
	 * backend processes expect the message terminator to be.  (However, other
	 * TCP servers may not necessarily expect this.)
	 *
	 * The <code>SocketChannel</code> is put into blocking mode before the write
	 * (sending messages in blocking mode is faster).  Other code that wants it
	 * in non-blocking mode is responsible for resetting this.
	 *
	 * @param message The message to be sent.
	 * @param immediateFlush Should the socket be flushed immediately - it's
	 *                       useful to set this to false if another sendMessage
	 *                       call is going to be made immediately after this one
	 * @return The number of milliseconds for which the method slept due to back
	 *         pressure.
	 */
	private long sendMessage(String message,
							boolean immediateFlush) throws IOException
	{
		m_isSendingMsg = true;
		try
		{
			long sleepTime = 0;


			// First, if we're accepting back pressure and have been told to slow
			// down, do that
			if (m_AcceptingBackPressure && m_SlowDownTimeMS > 0)
			{
				try
				{
					sleepTime = m_SlowDownTimeMS;
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					s_Logger.warn("Interrupted during back pressure sleep");

					// Complete guess!
					sleepTime /= 2;
				}
			}

			for (int attempt = 0; attempt <= RETRIES; ++attempt)
			{
				try
				{
					if (m_SocketChannel == null || !m_SocketChannel.isConnected())
					{
						if (openConnection() == false)
						{
							// Each time we fail to open the socket, back off for a
							// few seconds to give the server a chance to come back
							// up
							try
							{
								Thread.sleep(RETRY_DELAY_MS);
							}
							catch (InterruptedException e)
							{
								break;
							}
						}
					}

					if (m_SocketChannel != null && m_SocketChannel.isConnected())
					{
						// The back end's XML parser works with the UTF-8 character
						// set, whereas Java uses UTF-16, so we need to convert the
						// string
						byte[] backendChars = message.getBytes(BACKEND_CHARSET);

						m_SocketChannel.configureBlocking(true);

						m_BufferedOutput.write(backendChars);
						m_BufferedOutput.write(BACKEND_MSG_TERMINATOR);

						// Flush buffer if we've been instructed to do so, or if
						// we're slowing down.  If we're slowing down then there'll
						// be a delay before the next message anyway, and the
						// connection is more likely to be dropped before the next
						// message is sent.
						if (immediateFlush ||
								(m_AcceptingBackPressure && m_SlowDownTimeMS > 0))
						{
							m_BufferedOutput.flush();
						}

						// Successfully sent
						return sleepTime;
					}
				}
				catch (UnsupportedEncodingException e)
				{
					// As long as Prelert's wire character set is UTF-8, this should
					// never happen according to the JVM specification
					s_Logger.error(BACKEND_CHARSET + " character set not supported");

					// Re-throw immediately in this case
					throw e;
				}
				catch (IOException e)
				{
					s_Logger.error("Prelert TCP client failed to send message to host=" +
							m_Host + ", port=" + m_Port + " : " + e);

					closeConnection();
				}

				if (m_Quit)
				{
					break;
				}

				if (attempt < RETRIES)
				{
					s_Logger.info("Will retry " + (RETRIES - attempt) +
					" more times");
				}
			}
		}
		finally 
		{
			m_isSendingMsg = false;
		}

		throw new IOException("Message not sent and lost: " + message);
	}


	/**
	 * Opens a socket connection on the host and port specified
	 * to the constructor.
	 * @return true if the socket was opened successfully.
	 */
	synchronized private boolean openConnection()
	{
		if (m_SocketChannel != null && m_SocketChannel.isConnected())
		{
			// Check for inconsistent state
			if (m_BufferedOutput != null)
			{
				return true;
			}

			s_Logger.error("Inconsistency - socket channel open but buffered output is null");
		}

		s_Logger.info("Prelert TCP client attempting to open a connection to server on host " +
				m_Host + ", port " + m_Port);

		// Clean up any buffer left lingering by previous errors
		if (m_BufferedOutput != null)
		{
			try
			{
				m_BufferedOutput.close();
			}
			catch (IOException e)
			{
				s_Logger.error(e);
			}
			m_BufferedOutput = null;
		}

		// Clean up any existing but not connected socket channel
		if (m_SocketChannel != null)
		{
			try
			{
				m_SocketChannel.close();
			}
			catch (IOException e)
			{
				s_Logger.error(e);
			}
			m_SocketChannel = null;
		}

		// Now we've definitely got a clean slate, try to start up a new
		// connection
		try
		{
			m_SocketChannel = SocketChannel.open();

			// Kick off connection establishment
			m_SocketChannel.connect(new InetSocketAddress(m_Host, m_Port));
			m_SocketChannel.finishConnect();

			if (m_SocketChannel.isConnected())
			{
				OutputStream oStream = Channels.newOutputStream(m_SocketChannel);
				m_BufferedOutput = new BufferedOutputStream(oStream);

				// If we're accepting back pressure, send the special command to
				// the server to tell it that
				if (m_AcceptingBackPressure)
				{
					sendMessage(CMD_ACCEPT_BACK_PRESSURE, true);
				}

				s_Logger.info("Prelert TCP client successfully opened a connection to server on host " +
							m_Host + ", port " + m_Port);
			}
		}
		catch (IOException e)
		{
			s_Logger.error("Prelert TCP client could not open a connection to server on host " +
					m_Host + ", port " + m_Port + " : " + e);

			m_SocketChannel = null;

			// If we're waiting for all messages to be sent, but the process
			// we're sending to has died, lose the data.  This prevents us from
			// hanging for an incredibly long time (while every single queued
			// message waits for its retries) when instructed to shut down.
			if (m_WaitingForEmptyQueue)
			{
				s_Logger.error("Connection to server on host " +
							m_Host + ", port " + m_Port +
							" lost while waiting for empty queue - discarding " +
							m_MessageQueue.size() + " messages");
				m_MessageQueue.clear();
			}
			return false;
		}

		// Reset the back pressure slowdown to zero each time we open the
		// connection.  If the server we were talking to crashed and a new one
		// has replaced it, that new server won't know the old server had asked
		// us to slow down, so will just think we're permanently slow.
		m_SlowDownTimeMS = 0;

		// Reset the feedback command buffer, as any leftovers in the old buffer
		// relate to an old socket
		m_CommandBuffer.clear();

		return m_SocketChannel.isConnected();
	}


	/**
	 * Closes the socket connection.
	 * @return true if the socket needed to be closed.
	 */
	synchronized private boolean closeConnection()
	{
		if (m_SocketChannel == null)
		{
			// Check for inconsistent state
			if (m_BufferedOutput != null)
			{
				s_Logger.error("Inconsistency - socket channel null but buffered output is not null");

				try
				{
					m_BufferedOutput.close();
				}
				catch (IOException e)
				{
					s_Logger.error(e);
				}
				m_BufferedOutput = null;
			}

			m_CommandBuffer.clear();

			return false;
		}

		if (m_SocketChannel.socket().isClosed())
		{
			if (m_BufferedOutput != null)
			{
				try
				{
					m_BufferedOutput.close();
				}
				catch (IOException e)
				{
					s_Logger.error(e);
				}
				m_BufferedOutput = null;
			}

			m_SocketChannel = null;

			m_CommandBuffer.clear();

			return false;
		}

		s_Logger.info("Closing TCP connection to server on host " +
						m_Host + ", port " + m_Port);

		try
		{
			m_SocketChannel.configureBlocking(true);
		}
		catch (IOException e)
		{
			s_Logger.error(e);
		}

		if (m_BufferedOutput != null)
		{
			try
			{
				m_BufferedOutput.close();
			}
			catch (IOException e)
			{
				s_Logger.error(e);
			}
			m_BufferedOutput = null;
		}

		try
		{
			m_SocketChannel.close();
			m_SocketChannel = null;
		}
		catch (IOException e)
		{
			s_Logger.error(e);
		}

		m_CommandBuffer.clear();

		return true;
	}


	/**
	 * Return the back pressure that has been applied by the server.
	 * @return
	 */
	public long getSlowDownTimeMS()
	{
		return m_SlowDownTimeMS;
	}


	/**
	 * Returns the host this object opened a TCP connection to.
	 * @return
	 */
	public String getHost()
	{
		return m_Host;
	}


	/**
	 * Returns the port the TCP connection was opened on.
	 * @return
	 */
	public int getPort()
	{
		return m_Port;
	}

}

