package net.hep.ami.task;

import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import net.hep.ami.mini.*;

public class Main implements AMIHandler
{
	/*---------------------------------------------------------------------*/

	private static final int MAX_TASKS_DEFAULT = 10;

	private static final float COMPRESSION_DEFAULT = 2.0f;

	private static final Pattern s_ipSplitPattern = Pattern.compile("[^0-9\\.]");

	/*---------------------------------------------------------------------*/

	private Scheduler m_scheduler;

	private AMIServer m_amiServer;

	/*---------------------------------------------------------------------*/

	@Override
	public void init(Map<String, String> config) throws Exception
	{
		String s;

		/*-----------------------------------------------------------------*/
		/* CHECK TASK SERVER INFORMATION                                   */
		/*-----------------------------------------------------------------*/

		String jdbcUrl            = config.get("jdbc_url"   );
		String routerUser         = config.get("router_user");
		String routerPass         = config.get("router_pass");
		String exclusionServerUrl = config.get("exclusion_server_url");
		String serverName         = config.get("server_name");

		if(jdbcUrl            == null
		   ||
		   routerUser         == null
		   ||
		   routerPass         == null
		   ||
		   exclusionServerUrl == null
		   ||
		   serverName         == null
		 ) {
			throw new Exception("config error");
		}

		/*-----------------------------------------------------------------*/

		s = config.get("max_tasks");

		int maxTasks = (s != null) ? Integer.parseInt(s) : MAX_TASKS_DEFAULT;

		if(maxTasks < 1)
		{
			throw new Exception("`max_tasks` out of range");
		}

		/*-----------------------------------------------------------------*/

		s = config.get("compression");

		Float compression = (s != null) ? Float.parseFloat(s) : COMPRESSION_DEFAULT;

		if(compression < 1.0)
		{
			throw new Exception("`compression` out of range");
		}

		/*-----------------------------------------------------------------*/
		/* RUN SCHEDULER                                                   */
		/*-----------------------------------------------------------------*/

		(m_scheduler = new Scheduler(jdbcUrl, routerUser, routerPass, exclusionServerUrl, serverName, maxTasks, compression)).start();

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	@Override
	public StringBuilder exec(Map<String, String> config, String command, Map<String, String> arguments, String ip, String clientDN, String issuerDN) throws Exception
	{
		StringBuilder result = new StringBuilder();

		result.append("<info><![CDATA[done with success]]></info>");

		/*-----------------------------------------------------------------*/
		/* GetSessionInfo                                                  */
		/*-----------------------------------------------------------------*/

		/**/ if(command.equals("GetSessionInfo"))
		{
			result.append("<rowset type=\"user\">")
			      .append("<row>")
			      .append("<field name=\"valid\"><![CDATA[true]]></field>")
			      .append("<field name=\"AMIUser\"><![CDATA[admin]]></field>")
			      .append("<field name=\"guestUser\"><![CDATA[guest]]></field>")
			      .append("<field name=\"lastName\"><![CDATA[admin]]></field>")
			      .append("<field name=\"firstName\"><![CDATA[admin]]></field>")
			      .append("<field name=\"email\"><![CDATA[none]]></field>")
			      .append("</row>")
			      .append("</rowset>")
			;
		}

		/*-----------------------------------------------------------------*/
		/* GetTaskStatus                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("GetTasksStatus"))
		{
			result.append("<rowset>");

			for(Map<String, String> map: m_scheduler.getTasksStatus())
			{
				result.append("<row>")
				      .append("<field name=\"id\"><![CDATA[").append(map.get("id")).append("]]></field>")
				      .append("<field name=\"name\"><![CDATA[").append(map.get("name")).append("]]></field>")
				      .append("<field name=\"command\"><![CDATA[").append(map.get("command")).append("]]></field>")
				      .append("<field name=\"description\"><![CDATA[").append(map.get("description")).append("]]></field>")
				      .append("<field name=\"commaSeparatedLocks\"><![CDATA[").append(map.get("commaSeparatedLocks")).append("]]></field>")
				      .append("<field name=\"running\"><![CDATA[").append(map.get("running")).append("]]></field>")
				      .append("<field name=\"success\"><![CDATA[").append(map.get("success")).append("]]></field>")
				      .append("<field name=\"priority\"><![CDATA[").append(map.get("priority")).append("]]></field>")
				      .append("<field name=\"step\"><![CDATA[").append(map.get("step")).append("]]></field>")
				      .append("<field name=\"lastRunDate\"><![CDATA[").append(map.get("lastRunDate")).append("]]></field>")
				      .append("</row>")
				;
			}

			result.append("</rowset>");
		}

		/*-----------------------------------------------------------------*/
		/* LockScheduler                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("LockScheduler"))
		{
			checkIP(config, ip);

			m_scheduler.lock();
		}

		/*-----------------------------------------------------------------*/
		/* UnlockScheduler                                                 */
		/*-----------------------------------------------------------------*/

		else if(command.equals("UnlockScheduler"))
		{
			checkIP(config, ip);

			m_scheduler.unlock();
		}

		/*-----------------------------------------------------------------*/
		/* StopServer                                                      */
		/*-----------------------------------------------------------------*/

		else if(command.equals("StopServer"))
		{
			checkIP(config, ip);

			m_scheduler.gracefulStop();

			m_amiServer.gracefulStop();
		}

		/*-----------------------------------------------------------------*/

		else
		{
			throw new Exception("command `" + command + "` not found");
		}

		return result;
	}

	/*---------------------------------------------------------------------*/

	@Override
	public StringBuilder help(Map<String, String> config, String command, String ip, String clientDN, String issuerDN) throws Exception
	{
		StringBuilder result = new StringBuilder();

		/*-----------------------------------------------------------------*/
		/* GetSessionInfo                                                  */
		/*-----------------------------------------------------------------*/

		/**/ if(command.equals("GetSessionInfo"))
		{
			result.append("Get session info");
		}

		/*-----------------------------------------------------------------*/
		/* GetTaskStatus                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("GetTasksStatus"))
		{
			checkIP(config, ip);

			result.append("Get task status");
		}

		/*-----------------------------------------------------------------*/
		/* LockScheduler                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("LockScheduler"))
		{
			checkIP(config, ip);

			result.append("Lock the scheduler");
		}

		/*-----------------------------------------------------------------*/
		/* UnlockScheduler                                                 */
		/*-----------------------------------------------------------------*/

		else if(command.equals("UnlockScheduler"))
		{
			checkIP(config, ip);

			result.append("Unlock the scheduler");
		}

		/*-----------------------------------------------------------------*/
		/* StopServer                                                      */
		/*-----------------------------------------------------------------*/

		else if(command.equals("StopServer"))
		{
			checkIP(config, ip);

			result.append("Stop the server");
		}

		/*-----------------------------------------------------------------*/

		else
		{
			throw new Exception("Command not found");
		}

		return result;
	}

	/*---------------------------------------------------------------------*/

	@Override
	public void join() throws InterruptedException
	{
		m_scheduler.join();
	}

	/*---------------------------------------------------------------------*/

	@Override
	public StringBuilder usage(Map<String, String> config, String command, String ip, String clientDN, String issuerDN) throws Exception
	{
		return null;
	}

	/*---------------------------------------------------------------------*/

	private void checkIP(Map<String, String> config, String ip) throws Exception
	{
		/*-----------------------------------------------------------------*/

		String commaSeparatedIPs = config.get("ips");

		if(commaSeparatedIPs == null)
		{
			return;
		}

		/*-----------------------------------------------------------------*/

		for(String IP: s_ipSplitPattern.split(commaSeparatedIPs))
		{
			if(ip.equals(IP))
			{
				return;
			}
		}

		/*-----------------------------------------------------------------*/

		throw new Exception("user not allowed");

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	public static void main(String[] args)
	{
		try
		{
			Main myHandler = new Main();

			myHandler.m_amiServer = new AMIServer(args.length == 1 ? Integer.parseInt(args[0]) : 1357, myHandler);

			myHandler.m_amiServer.start();
			myHandler.m_amiServer.join();
		}
		catch(Exception e)
		{
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE,
				e.getMessage(), e
			);

			System.exit(1);
		}

		System.exit(0);
	}

	/*---------------------------------------------------------------------*/
}
