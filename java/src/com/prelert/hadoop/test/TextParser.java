package com.prelert.hadoop.test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


public class TextParser 
{
	public static final String NAMED_CAPTURE_GROUP = "\\(\\?<(\\w+)>.*?\\)";
	private static final Logger s_Logger = Logger.getLogger(TextParser.class);
	
	
	private String m_Regex;
	private String m_AnonymousGroups;
	
	private Pattern m_NamedCaptureGroupRegex;
	
	private List<String> m_NamedCaptureGroups;
	
	
	private String contents = "_time	url	hitcount	sourcetype\n" +
"1293840000	0_(number)	205	wiki_traffic\n" +
"1293840000	1000_Ways_to_Die	624	wiki_traffic\n" +
"1293840000	1080p	224	wiki_traffic\n" +
"1293840000	112th_United_States_Congress	129	wiki_traffic\n" +
"1293840000	127_Hours	451	wiki_traffic\n" +
"1293840000	12-hour_clock	112	wiki_traffic\n" + 
"1293840000	16_and_Pregnant	100	wiki_traffic\n" +
"1293840000	1960s	112	wiki_traffic\n" +
"1293840000	1988_Notre_Dame_vs._Miami_football_game	110	wiki_traffic";
	
	public TextParser(String regex)
	{
		m_Regex = regex;
		
		m_NamedCaptureGroupRegex = Pattern.compile(NAMED_CAPTURE_GROUP);
		
		
		m_NamedCaptureGroups = namedCaptureGroups(m_Regex);
		m_AnonymousGroups = anonymiseNamedGroups(m_Regex, m_NamedCaptureGroups);
		
		
		parse(contents);
	}
	
	
	private List<String> namedCaptureGroups(String regex)
	{
		Matcher matches = m_NamedCaptureGroupRegex.matcher(regex);
		
		List<String> namedGroups = new ArrayList<String>();
		
		int index = 0;
		while (matches.find(index))
		{
			int groupCount = matches.groupCount(); 
						
			for (int i=1; i<=groupCount; i++)
			{
				namedGroups.add(matches.group(i));
			}

			index = matches.end();
		}
		
		return namedGroups;
	}
	
	/**
	 * Named capture groups are only supported in Java 7 as we are likely to be
	 * on Java 6 strip out the names. The regex can still be used with anonymous
	 * capture groups.
	 *  
	 * @return
	 */
	private String anonymiseNamedGroups(String regex, List<String> namedGroups)
	{
		for (String ng : namedGroups)
		{
			String group = "\\?<" + ng + ">";
			regex = regex.replaceAll(group, "");
		}
		
		return regex;
	}
	
	
	
	public void parse(String content)
	{
		Pattern p = Pattern.compile(m_AnonymousGroups);
		
		String [] lines = content.split("\n");
		for (int i=0; i<lines.length; i++)
		{
			Matcher m = p.matcher(lines[i]);
			if (m.matches())
			{
				for (int j=1; j<=m.groupCount(); j++)
				{
					s_Logger.info(m.group(j));
				}
				s_Logger.info("");
			}
			
		}
	}
	
	
	public String getRegex()
	{
		return m_Regex;
	}
	
	public String getAnonymousRegex()
	{
		return m_AnonymousGroups;
	}
	
	public List<String> getNamedCaptureGroups()
	{
		return m_NamedCaptureGroups;
	}
	

	
	
	public static void main(String[] args)
	{
		s_Logger.info(NAMED_CAPTURE_GROUP);
		
		String regex = "(?<projectcode>\\S+)\\t(?<url>\\S+)\\t(?<hitcount>\\d+)\\t(?<bytecount>\\d+)";
				
		
		TextParser tp = new TextParser(regex);
		for (String name : tp.getNamedCaptureGroups())
		{
			s_Logger.info(name);
		}
		
		s_Logger.info(tp.getRegex());
		s_Logger.info(tp.getAnonymousRegex());
		
		
		
	}
	
}
