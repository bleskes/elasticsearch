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

package com.prelert.proxy.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.prelert.proxy.inputmanager.DataCollectionMode;
import com.prelert.proxy.inputmanager.PrelertBackendTCPClient;


/**
 * This class is designed to exercise the PrelertBackendTCPClient, to ensure
 * it's robust in high-throughput environments.
 */
public class TCPClientTest
{
	static Logger s_Logger = Logger.getLogger(TCPClientTest.class);

	private static final int TEST_PORT = 45678;

	/**
	 * Test that we can pump a large number of reasonably big messages (up to
	 * 16K in size) through the TCP client in a reasonable amount of time.
	 *
	 * At the end of the test, neither the server nor the client should be
	 * running, even if the test fails.
	 *
	 * Note that the test may fail intermittantly on virtual machines if the
	 * hypervisor stalls the VM for too long during the test, e.g. for host
	 * level swapping.  If a failure is caused by a transient problem like this,
	 * the test should succeed if run again.
	 */
	@Test
	public void testMaxThroughput()
	throws InterruptedException, IOException
	{
		s_Logger.info("Starting testMaxThroughput test");

		final int TEST_SIZE = 10000;

		TestTCPServer server = this.new TestTCPServer();
		server.start();
		try
		{
			// Give the server a chance to start up
			Thread.sleep(1000);

			PrelertBackendTCPClient client = new PrelertBackendTCPClient("localhost",
                                    									TEST_PORT,
                                    									true,
                                    									DataCollectionMode.HISTORICAL);
			client.start();
			try
			{
				Random generator = new Random();

				// Queue messages of varying sizes for sending
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					// Message will contain up to 16K of junk
					int junkSize = 1 + generator.nextInt(16383);
					char junk[] = new char[junkSize];
					Arrays.fill(junk, 'a');

					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);
					builder.append(" - ");
					builder.append(junk);

					client.queueMessage(builder.toString());
				}

				// Give the server a chance to receive the messages
				Thread.sleep(1000);

				assertEquals(TEST_SIZE, server.getMessagesReceivedCount());
			}
			finally
			{
				client.quit();
			}
		}
		finally
		{
			server.shutdown();
		}

		s_Logger.info("Finished testMaxThroughput test");
	}


	/**
	 * Test that the client correctly reacts to back-pressure requests that
	 * Prelert servers can send, including edge cases where back-pressure
	 * requests are received in quick succession or split between multiple
	 * socket reads.
	 *
	 * At the end of the test, neither the server nor the client should be
	 * running, even if the test fails.
	 *
	 * Note that the test may fail intermittantly on virtual machines if the
	 * hypervisor stalls the VM for too long during the test, e.g. for host
	 * level swapping.  If a failure is caused by a transient problem like this,
	 * the test should succeed if run again.
	 */
	@Test
	public void testBackPressure()
	throws InterruptedException, IOException, UnsupportedEncodingException
	{
		s_Logger.info("Starting testBackPressure test");

		final int TEST_SIZE = 50;
		final char TERMINATOR_CHAR = (char)PrelertBackendTCPClient.BACKEND_MSG_TERMINATOR;

		// The logic below assumes the test size is bigger than 10
		assertTrue(TEST_SIZE > 10);

		TestTCPServer server = this.new TestTCPServer();
		server.start();
		try
		{
			// Give the server a chance to start up
			Thread.sleep(1000);

			PrelertBackendTCPClient client = new PrelertBackendTCPClient("localhost",
                                    									TEST_PORT,
                                    									true,
                                    									DataCollectionMode.HISTORICAL);
			client.start();
			try
			{
				// First send some messages fast
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				// Give the server a chance to receive the messages
				Thread.sleep(1000);

				assertEquals(TEST_SIZE, server.getMessagesReceivedCount());

				// Now tell the client to slow down by 100ms
				String cmd = PrelertBackendTCPClient.CMD_SLOW_DOWN + "100"
							+ TERMINATOR_CHAR;
				server.sendReturnMessage(cmd.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET));

				// Give the client a chance to react to the command
				Thread.sleep(1000);

				// Now send some more messages - it should take about 5 seconds before
				// the server receives them all
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				Thread.sleep(TEST_SIZE * 100 - 1000);

				// At this point, the server should NOT have received all the messages
				assertTrue(server.getMessagesReceivedCount() < TEST_SIZE * 2);

				Thread.sleep(2000);

				// At this point, the server SHOULD have received all the messages
				assertEquals(TEST_SIZE * 2, server.getMessagesReceivedCount());

				// Now tell the client to speed up again
				cmd = PrelertBackendTCPClient.CMD_SPEED_UP + TERMINATOR_CHAR;
				server.sendReturnMessage(cmd.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET));

				// Give the client a chance to react to the command
				Thread.sleep(1000);

				// Now send some more messages fast
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				// Give the server a chance to receive the messages
				Thread.sleep(1000);

				assertEquals(TEST_SIZE * 3, server.getMessagesReceivedCount());

				// Now a more complex case - we tell the client to slow down by first
				// 10ms, then 200ms, simulating this happening so quickly that both
				// messages are received on the client's socket in the same read
				// operation
				cmd = PrelertBackendTCPClient.CMD_SLOW_DOWN + "10" + TERMINATOR_CHAR +
						PrelertBackendTCPClient.CMD_SLOW_DOWN + "200" + TERMINATOR_CHAR;
				server.sendReturnMessage(cmd.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET));

				// Give the client a chance to react to the command
				Thread.sleep(1000);

				// Now send some more messages - it should take about 10 seconds before
				// the server receives them all
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				Thread.sleep(TEST_SIZE * 200 - 1000);

				// At this point, the server should NOT have received all the messages
				assertTrue(server.getMessagesReceivedCount() < TEST_SIZE * 4);

				Thread.sleep(2000);

				// At this point, the server SHOULD have received all the messages
				assertEquals(TEST_SIZE * 4, server.getMessagesReceivedCount());

				// Now tell the client to speed up again, this time simulating a
				// previous message arriving in bits, with the speed up arriving in
				// quick succession to the last bit of this previous message
				cmd = PrelertBackendTCPClient.CMD_SLOW_DOWN; // NB: unterminated
				server.sendReturnMessage(cmd.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET));

				// Give the client a chance to react to the partial command
				Thread.sleep(1000);

				cmd = "300" + TERMINATOR_CHAR +
						PrelertBackendTCPClient.CMD_SPEED_UP + TERMINATOR_CHAR;
				server.sendReturnMessage(cmd.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET));

				// Give the client a chance to react to the rest of the commands
				Thread.sleep(1000);

				// Now send some more messages fast
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				// Give the server a chance to receive the messages
				Thread.sleep(1000);

				assertEquals(TEST_SIZE * 5, server.getMessagesReceivedCount());
			}
			finally
			{
				client.quit();
			}
		}
		finally
		{
			server.shutdown();
		}

		s_Logger.info("Finished testBackPressure test");
	}


	/**
	 * Test that if the server dies, the client tries to reconnect, and succeeds
	 * in doing so once the server is alive again.
	 *
	 * At the end of the test, neither of the servers nor the client should be
	 * running, even if the test fails.
	 *
	 * Note that the test may fail intermittantly on virtual machines if the
	 * hypervisor stalls the VM for too long during the test, e.g. for host
	 * level swapping.  If a failure is caused by a transient problem like this,
	 * the test should succeed if run again.
	 */
	@Test
	public void testReconnect()
	throws Exception
	{
		s_Logger.info("Starting testReconnect test");

		final int TEST_SIZE = 5;

		// The logic below assumes the test size is at least 3
		assertTrue(TEST_SIZE >= 3);

		// The same client is used with two servers, so declare the variable
		// outside the try block
		PrelertBackendTCPClient client = null;
		boolean clientIsStarted = false;

		try
		{
			TestTCPServer server1 = this.new TestTCPServer();
			server1.start();
			try
			{
				// Give the server a chance to start up
				Thread.sleep(1000);

				client = new PrelertBackendTCPClient("localhost",
                                    				TEST_PORT,
                                    				true,
                                    				DataCollectionMode.HISTORICAL);
				client.start();
				clientIsStarted = true;

				// Send some messages
				for (int count = 0; count < TEST_SIZE; ++count)
				{
					StringBuilder builder = new StringBuilder("Message ");
					builder.append(count);

					client.queueMessage(builder.toString());
				}

				// Give the server a chance to receive the messages
				Thread.sleep(1000);

				// The server should have received the messages
				assertEquals(TEST_SIZE, server1.getMessagesReceivedCount());

				// The first server dies
				s_Logger.info("Simulating server death");
			}
			finally
			{
				server1.shutdown();
			}

			// Give the server a chance to shut down
			Thread.sleep(1000);

			// Give the client a chance to realise the server has died
			Thread.sleep(1000);

			// Queue some more messages
			for (int count = 0; count < TEST_SIZE; ++count)
			{
				StringBuilder builder = new StringBuilder("Message ");
				builder.append(count);

				client.queueMessage(builder.toString());
			}

			// Wait long enough that one message will be lost
			Thread.sleep(1000 + (1 + PrelertBackendTCPClient.RETRIES) * PrelertBackendTCPClient.RETRY_DELAY_MS);

			// Wait long enough that the next message will have used up some but
			// not all of its retries (either 1 or 2 assuming the retry delay is
			// greater than 1 second)
			assertTrue(PrelertBackendTCPClient.RETRIES >= 2);
			assertTrue(PrelertBackendTCPClient.RETRY_DELAY_MS > 1000);
			Thread.sleep(1000 + PrelertBackendTCPClient.RETRY_DELAY_MS);
		}
		catch (Exception e)
		{
			// Stop the client if it's running, then propagate the exception
			if (clientIsStarted)
			{
				client.quit();
			}

			throw e;
		}

		// The server comes back up
		s_Logger.info("Simulating server restart");
		TestTCPServer server2 = this.new TestTCPServer();
		try
		{
			server2.start();
		}
		catch (Exception e)
		{
			// Stop the client if it's running, then propagate the exception
			if (clientIsStarted)
			{
				client.quit();
			}

			throw e;
		}

		try
		{
			// Give the new server a chance to start up, and the client time for
			// another retry to definitely occur
			Thread.sleep(1000 + 2 * PrelertBackendTCPClient.RETRY_DELAY_MS);

			// The second server should have received one message less than the test
			// size, as we lost one due to the length of time the server was down
			assertEquals(TEST_SIZE - 1, server2.getMessagesReceivedCount());
		}
		finally
		{
			client.quit();

			server2.shutdown();
		}

		s_Logger.info("Finished testReconnect test");
	}


	/**
	 * This class attempts to mimic the behaviour of the Prelert C++ TCP server,
	 * in order to give some confidence that the Java TCP client will be able to
	 * reliably connect to it.
	 */
	class TestTCPServer implements Runnable
	{
		private Thread m_Thread;
		volatile private boolean m_Quit;

		private ServerSocketChannel m_ServerChannel;
		private Selector m_Selector;

		/**
		 * This member stores the return channel of the most recently accepted
		 * connection.  For this unit test class, this works because there is
		 * only ever one connection.  (Obviously it wouldn't be acceptable for
		 * a production TCP server class!)
		 */
		private SocketChannel m_ReturnChannel;

		/**
		 * This needs to be volatile as it's written and read from different
		 * threads
		 */
		volatile private int m_MessagesReceivedCount;


		/**
		 * The socket is bound in the start() method rather than in the
		 * constructor, to allow the server to be stopped and restarted
		 * multiple times if required.
		 */
		public TestTCPServer()
		{
			m_Thread = new Thread(this, "TestTCPServer");

			m_MessagesReceivedCount = 0;
		}


		/**
		 * How many messages has this server received (excluding control
		 * commands)?
		 */
		public int getMessagesReceivedCount()
		{
			return m_MessagesReceivedCount;
		}


		/**
		 * Send data back to the client.
		 * @param message The message to be returned as a byte array rather than
		 *                a String.  This enables the caller to mess up the
		 *                encoding and/or send mulitple messages in the same
		 *                data block to increase the chance that they'll be
		 *                received on the socket at the same time.
		 */
		synchronized public void sendReturnMessage(byte[] message) throws IOException
		{
			if (m_ReturnChannel != null)
			{
				// This is inefficient - in a production TCP server class the
				// buffer would be persisted between calls
				ByteBuffer buf = ByteBuffer.allocate(message.length);
				buf.put(message);
				buf.flip();

				// Since the channel is non-blocking, we have to write in a
				// loop, because each write might only write part of the buffer.
				// (It's against the rules to set it to blocking whilst we
				// write, unless we de-register it from the selector first.)
				while (buf.hasRemaining())
				{
					m_ReturnChannel.write(buf);
				}
			}
		}


		/**
		 * Thread start function.  Should only be called by the m_Thread member.
		 */
		@Override
		public void run()
		{
			// This loop will be terminated when the selector is closed
			for (;;)
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
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();

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

							// Store the return channel of this connection if
							// we're not shutting down
							if (!m_Quit)
							{
								// This will deadlock if we're shutting down,
								// hence the check immediately above
								synchronized (this)
								{
									m_ReturnChannel = socketChannel;
								}
							}
						}
						else if (key.isReadable())
						{
							readMessages(key);
						}
					}
				}
				catch (IOException e)
				{
					TCPClientTest.s_Logger.info("Caught java.io.IOException: " + e);
				}
				catch (ClosedSelectorException e)
				{
					if (!m_Quit) // only log error if not quitting.
					{
						TCPClientTest.s_Logger.info("Selector closed : " + e);
					}
					break;
				}
			}
		}


		/**
		 * Read messages from the socket, and count how many there are.
		 * Control commands are NOT counted as messages.
		 */
		private void readMessages(SelectionKey key) throws IOException
		{
			final int BUFFER_SIZE = 1024;

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

			if (!byteBuffer.hasArray())
			{
				if (numRead > 0)
				{
					TCPClientTest.s_Logger.error("Inconsistency - " + numRead +
									" bytes read from socket but byte buffer has no array");
				}
				return;
			}

			// Each message ends with a zero byte, so increment the message
			// count each time we see one of these
			for (int i = 0; i < byteBuffer.position(); ++i)
			{
				if (byteBuffer.get(i) == PrelertBackendTCPClient.BACKEND_MSG_TERMINATOR)
				{
					// EXCEPT ignore back pressure acceptance commands
					if (i == PrelertBackendTCPClient.CMD_ACCEPT_BACK_PRESSURE.length())
					{
						try
						{
							byte[] abpCmdBytes = PrelertBackendTCPClient.CMD_ACCEPT_BACK_PRESSURE.getBytes(PrelertBackendTCPClient.BACKEND_CHARSET);
							boolean matchAbpCmd = true;
							for (int pos = 0; pos < i; ++pos)
							{
								if (byteBuffer.get(pos) != abpCmdBytes[pos])
								{
									matchAbpCmd = false;
									break;
								}
							}

							if (matchAbpCmd)
							{
								continue;
							}
						}
						catch (UnsupportedEncodingException e)
						{
							// As long as Prelert's wire character set is UTF-8,
							// this should never happen according to the JVM
							// specification
							s_Logger.error(PrelertBackendTCPClient.BACKEND_CHARSET +
											" character set not supported");
						}
					}
					++m_MessagesReceivedCount;
				}
			}

			byteBuffer.clear();
		}


		/**
		 * Start the TCP server.
		 */
		synchronized public void start() throws IOException
		{
			if (m_Thread.isAlive())
			{
				throw new UnsupportedOperationException("Cannot start server that is already running");
			}

			m_Quit = false;

			// Create a new non-blocking server socket channel
			m_ServerChannel = ServerSocketChannel.open();
			m_ServerChannel.configureBlocking(false);

			InetSocketAddress isa = new InetSocketAddress(TCPClientTest.TEST_PORT);
			m_ServerChannel.socket().bind(isa);

			try
			{
				// Register the server socket channel, indicating an interest in
				// accepting new connections
				m_Selector = SelectorProvider.provider().openSelector();
				m_ServerChannel.register(m_Selector, SelectionKey.OP_ACCEPT);

				m_Thread.start();
			}
			catch (IOException e)
			{
				// Release the port we bound to if an IO exception occurs
				m_ServerChannel.close();
				throw e;
			}
			catch (RuntimeException e)
			{
				// Release the port we bound to if an unexpected exception
				// occurs
				m_ServerChannel.close();
				throw e;
			}
		}


		/**
		 * Shut down the TCP server.
		 */
		synchronized public void shutdown()
		{
			if (!m_Thread.isAlive())
			{
				throw new UnsupportedOperationException("Cannot shut down server that is not running");
			}

			m_Quit = true;
			try
			{
				m_Selector.close();
			}
			catch (IOException e)
			{
				TCPClientTest.s_Logger.error("IOException thrown when quitting server thread: " + e);
			}

			try
			{
				m_Thread.join();
			}
			catch (InterruptedException e)
			{
				TCPClientTest.s_Logger.error("InterruptedException thrown when waiting for server thread: " + e);
			}

			m_Selector = null;

			if (m_ReturnChannel != null)
			{
				try
				{
					m_ReturnChannel.close();
					m_ReturnChannel = null;
				}
				catch (IOException e)
				{
					TCPClientTest.s_Logger.error("IOException thrown when closing return channel: " + e);
				}
			}

			if (m_ServerChannel != null)
			{
				try
				{
					m_ServerChannel.close();
					m_ServerChannel = null;
				}
				catch (IOException e)
				{
					TCPClientTest.s_Logger.error("IOException thrown when closing server socket: " + e);
				}
			}
		}

	}

}

