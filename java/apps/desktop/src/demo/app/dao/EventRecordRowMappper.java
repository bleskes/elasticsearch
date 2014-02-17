package demo.app.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import demo.app.data.gxt.EvidenceModel;


/**
 * ParameterizedRowMapper class for mapping evidence query result sets to
 * EventRecord objects.
 */
public class EventRecordRowMappper implements ParameterizedRowMapper<EvidenceModel>
{
	static Logger logger = Logger.getLogger(TimeSeriesMySQLDAO.class);
	
	public EventRecordRowMappper()
	{
		
	}
	
	
    public EvidenceModel mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		EvidenceModel record = new EvidenceModel();
		
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
