package demo.app.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import demo.app.data.EventRecord;


/**
 * ParameterizedRowMapper class for mapping evidence query result sets to
 * EventRecord objects.
 */
public class EventRecordRowMappper implements ParameterizedRowMapper<EventRecord>
{
	static Logger logger = Logger.getLogger(TimeSeriesMySQLDAO.class);
	
	public EventRecordRowMappper()
	{
		
	}
	
	
    public EventRecord mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		EventRecord record = new EventRecord();
		
		ResultSetMetaData metaData = rs.getMetaData();

		String columnName;
		Object columnValue;
		for (int i = 1; i <= metaData.getColumnCount(); i++)
		{	
			columnName = metaData.getColumnLabel(i);
			columnValue = rs.getObject(i);
			
			record.set(columnName, columnValue);
		}

        return record;
    }

}
