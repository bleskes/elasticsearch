/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ************************************************************/

package com.prelert.api.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import org.apache.log4j.Logger;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StringLiteral;

/**
 * ODATA $filter query expressions parser as an implementation of the Visitor
 * pattern. This is an minimal implementation of the ExpressionVisitor interface
 * which only handles the following properties:
 * <ul>
 * <li>AnomalyScore</li>
 * <li>MetricPath</li>
 * <li>MPQuery</li>
 * <li>EscapeChar</li>
 * <li>PeakEvidenceTime</li
 * <li>FirstEvidenceTime</li>
 * <li>LastEvidenceTime</li>
 * <li>UpdateTime</li>
 * </ul>
 * Other expressions will not be parsed.<p/>
 * Only boolean comparison operators (eq, ge, gt, le, lt) and the And/Or operators
 * are supported
 * <p/>
 * Each node of the expression tree is visited by this class. 
 * 
 */
public class OdataExpressionVisitor extends org.odata4j.expression.FilterExpressionVisitor 
{
	static final private Logger s_Logger = Logger.getLogger(OdataExpressionVisitor.class);
	
	static final public String THRESHOLD_FILTER = "AnomalyScore";
	static final public String METRIC_PATH_FILTER = "MetricPath";
	static final public String PEAK_EVIDENCE_DATE_FILTER = "PeakEvidenceTime";
	static final public String FIRST_EVIDENCE_DATE_FILTER = "FirstEvidenceTime";
	static final public String LAST_EVIDENCE_DATE_FILTER = "LastEvidenceTime";
	static final public String UPDATE_DATE_FILTER = "UpdateTime";
	static final public String ESCAPE_CHAR_FILTER = "EscapeChar";
	static final public String METRIC_PATH_LIKE_FILTER = "MPQuery";
	

	/**
	 * The different expressions that may be parsed.
	 */
	public enum ExpressionType
	{
		LITERAL, PROPERTY, AND, OR, BOOLPAREN, 
		
		LT
		{
			@Override
			public boolean isComparison()
			{
				return true;
			}
		}, 
		
		LE
		{
			@Override
			public boolean isComparison()
			{
				return true;
			}
		}, 
		
		EQ
		{
			@Override
			public boolean isComparison()
			{
				return true;
			}
		}, 
		
		GE
		{
			@Override
			public boolean isComparison()
			{
				return true;
			}
		}, 
		
		GT
		{
			@Override
			public boolean isComparison()
			{
				return true;
			}
		};
		
		public boolean isComparison()
		{
			return false;
		}
	}
	
	
	
	/**
	 * Enum represents the possible date filter fields
	 */
	private enum DateFilter
	{
		PEAK_EVIDENCE, FIRST_EVIDENCE, LAST_EVIDENCE, UPDATE;
	}
	
	public class DateTimeFilter 
	{
		private Date m_DateTime;
		private LogicalOperator m_Op;
		
		public DateTimeFilter(Date date, LogicalOperator op)
		{
			this.m_DateTime = date;
			this.m_Op = op;
		}
		
		public Date getDateTime()
		{
			return this.m_DateTime;
		}
		
		public LogicalOperator getOperator()
		{
			return this.m_Op;
		}
		
	}
	
	private boolean m_IsThresholdFilter;
	private int m_ThresholdValue;

	private boolean m_IsMetricPathFilter;
	private List<String> m_MetricPathAnds;
	
	private boolean m_IsEscapeCharFilter;
	private String m_EscapeCharValue;
	
	private boolean m_IsMetricPathLikeFilter;
	private List<String> m_MetricPathLikeAnds;
	
	private DateTimeFilter m_MinPeakEvidenceTime;
	private DateTimeFilter m_MinFirstEvidenceTime;
	private DateTimeFilter m_MinLastEvidenceTime;
	private DateTimeFilter m_MinUpdateTime;
	
	private DateTimeFilter m_MaxPeakEvidenceTime;
	private DateTimeFilter m_MaxFirstEvidenceTime;
	private DateTimeFilter m_MaxLastEvidenceTime;
	private DateTimeFilter m_MaxUpdateTime;
	

	private String m_PropertyName;
	
	private int m_IntLiteral; 
	private long m_LongLiteral;
	private double m_DoubleLiteral; 
	private String m_StringLiteral; 
	private Date m_DateLiteral;
	
	
	private ExpressionType m_ExpressionType;
	
	private OdataExpressionVisitor m_CurrentExpression;

	private OdataExpressionVisitor m_lhs;
	private OdataExpressionVisitor m_rhs;
	
	private List<OdataExpressionVisitor> m_OrExpressions;
	private List<OdataExpressionVisitor> m_AndExpressions;
	
	private Deque<LogicalOperator> m_OperatorStack; 
	
	private Deque<OdataExpressionVisitor> m_ExpressionStack;
	
	public OdataExpressionVisitor()
	{
		m_OrExpressions = new ArrayList<OdataExpressionVisitor>();
		m_AndExpressions = new ArrayList<OdataExpressionVisitor>();
		
		m_OperatorStack = new ArrayDeque<LogicalOperator>();
		
		m_ExpressionStack = new ArrayDeque<OdataExpressionVisitor>();
		
		m_CurrentExpression = this;
		
		m_MetricPathAnds = new ArrayList<String>();
		m_MetricPathLikeAnds = new ArrayList<String>();
	}

	@Override
	public void visit(AndExpression expr) 
	{
		s_Logger.trace("AndExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.AND;
	}
	
	/**
	 * Only the metric path like queries can be joined by
	 * OR expressions
	 */
	@Override
	public void visit(OrExpression expr) 
	{
		s_Logger.trace("OrExpression");

		m_CurrentExpression.m_ExpressionType = ExpressionType.OR;
	}
	  

	@Override
	public void visit(BoolParenExpression expr) 
	{
		s_Logger.trace("BoolParenExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.BOOLPAREN;
	}

	@Override
	public void visit(EqExpression expr) 
	{
		s_Logger.trace("EqExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.EQ;
		
		m_CurrentExpression.m_OperatorStack.push(LogicalOperator.EQ);
	}
	  
	@Override
	public void visit(GeExpression expr) 
	{
		s_Logger.trace("GeExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.GE;
		
		m_CurrentExpression.m_OperatorStack.push(LogicalOperator.GE);
	}

	@Override
	public void visit(GtExpression expr) 
	{
		s_Logger.trace("GtExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.GT;
		
		m_CurrentExpression.m_OperatorStack.push(LogicalOperator.GT);
	}
	
	@Override
	public void visit(LeExpression expr) 
	{
		s_Logger.trace("LeExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LE;
		
		m_CurrentExpression.m_OperatorStack.push(LogicalOperator.LE);
	}

	@Override
	public void visit(LtExpression expr) 
	{
		s_Logger.trace("LtExpression");
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LT;

		m_CurrentExpression.m_OperatorStack.push(LogicalOperator.LT);
	}

	
	@Override
	public void visit(IntegralLiteral expr) 
	{
		s_Logger.trace("integral literal=" + expr.getValue());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;

		m_CurrentExpression.m_IntLiteral = expr.getValue();
	}

	@Override
	public void visit(Int64Literal expr) 
	{
		s_Logger.trace("int64 literal=" + expr.getValue());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_LongLiteral = (long)expr.getValue();
	}
	
	@Override
	public void visit(DoubleLiteral expr) 
	{
		s_Logger.trace("Doubleliteral=" + expr.getValue());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_DoubleLiteral = (double)expr.getValue();
	}
	
	@Override
	public void visit(SingleLiteral expr) 
	{
		s_Logger.trace("Singleliteral=" + expr.getValue());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_DoubleLiteral = (double)expr.getValue();
	}

	
	
	@Override
	public void visit(StringLiteral expr) 
	{
		s_Logger.trace("string literal=" + expr.getValue());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_StringLiteral = expr.getValue();
	}
	

	
	@Override
	public void visit(DateTimeLiteral expr) 
	{
		s_Logger.trace("DateTimeLiteral=" + expr);
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_DateLiteral = expr.getValue().toDateTime().toDate();
	}

	@Override
	public void visit(DateTimeOffsetLiteral expr) 
	{
		s_Logger.trace("DateTimeOffsetLiteral=" + expr);
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.LITERAL;
		
		m_CurrentExpression.m_DateLiteral = expr.getValue().toDate();
	}
	


	@Override
	public void visit(EntitySimpleProperty expr)
	{
		s_Logger.trace("simpleprop=" + expr.getPropertyName());
		
		m_CurrentExpression.m_ExpressionType = ExpressionType.PROPERTY;

		m_CurrentExpression.m_PropertyName = expr.getPropertyName();
	}
	

	/**
	 * Called before the visitor decends into a new expression.
	 * i.e before a (property eq value)
	 */
	@Override
	public void beforeDescend() 
	{
		s_Logger.trace("before descend");

		m_ExpressionStack.push(m_CurrentExpression);

		OdataExpressionVisitor newExp = new OdataExpressionVisitor();

		if (m_CurrentExpression.m_ExpressionType == ExpressionType.AND)
		{
			m_CurrentExpression.m_AndExpressions.add(newExp);
		}
		else if (m_CurrentExpression.m_ExpressionType == ExpressionType.OR)
		{
			m_CurrentExpression.m_OrExpressions.add(newExp);
		}
		else if (m_CurrentExpression.m_ExpressionType.isComparison())
		{
			m_CurrentExpression.m_lhs = newExp;
		}

		m_CurrentExpression = newExp;
	}


	/**
	 * called between an expression.
	 * i.e for (property eq value) called after property has been 
	 * evaluated but before value is.
	 */
	@Override
	public void betweenDescend() 
	{
		s_Logger.trace("between descend");

		m_CurrentExpression = m_ExpressionStack.pop();


		OdataExpressionVisitor newExp = new OdataExpressionVisitor();
		m_ExpressionStack.push(m_CurrentExpression);
		
		
		if (m_CurrentExpression.m_ExpressionType == ExpressionType.AND)
		{
			m_CurrentExpression.m_AndExpressions.add(newExp);
		}
		else if (m_CurrentExpression.m_ExpressionType == ExpressionType.OR)
		{
			m_CurrentExpression.m_OrExpressions.add(newExp);
		}
		else if (m_CurrentExpression.m_ExpressionType.isComparison())
		{
			m_CurrentExpression.m_rhs = newExp;
		}		
		
		m_CurrentExpression = newExp;
	}
	
	
	/**
	 * Called after an expression has been processed.
	 * i.e for (property eq value) this is called after both property 
	 * and value have been evaluated.
	 * 
	 * If a property name & value pair was set in this 
	 * descent set it.
	 */
	@Override
	public void afterDescend() 
	{
		s_Logger.trace("after descend");

		OdataExpressionVisitor lastExp = m_CurrentExpression;
		m_CurrentExpression = m_ExpressionStack.pop();
		
		
		if (m_CurrentExpression.m_ExpressionType == ExpressionType.AND)
		{
 			for (OdataExpressionVisitor visitor : m_CurrentExpression.m_AndExpressions)
			{
				mergeAndedExpressions(visitor, m_CurrentExpression);
			}
		}
		else if (m_CurrentExpression.m_ExpressionType == ExpressionType.OR)
		{
			mergeOrdedExpresssions(m_CurrentExpression);
		}
		else if (m_CurrentExpression.m_ExpressionType == ExpressionType.BOOLPAREN)
		{
			// merge simple assignment e.g metricpath eq 'a|b|c'
			if (lastExp.m_ExpressionType.isComparison()) 
			{
				mergeAndedExpressions(lastExp, m_CurrentExpression);
				m_CurrentExpression.m_ExpressionType = lastExp.m_ExpressionType;
			}
			else if (lastExp.m_ExpressionType == ExpressionType.OR)
			{
				m_CurrentExpression.m_OrExpressions = lastExp.m_OrExpressions;
				m_CurrentExpression.m_ExpressionType = lastExp.m_ExpressionType;
			}
			else if (lastExp.m_ExpressionType == ExpressionType.AND)
			{
				mergeAndedExpressions(lastExp, m_CurrentExpression);
				m_CurrentExpression.m_ExpressionType = lastExp.m_ExpressionType;
			}
		}
		else if (m_CurrentExpression.m_ExpressionType.isComparison())
		{
			setExpression(m_CurrentExpression.m_lhs, m_CurrentExpression.m_rhs, m_CurrentExpression.m_OperatorStack.pop());
		}

		
	}
	

	/**
	 * True if an anomaly score expression was parsed.
	 * @return
	 */
	public boolean isThresholdFilter()
	{
		return m_IsThresholdFilter;
	}
	
	/**
	 * True if an metric path expression was parsed.
	 * @return
	 */
	public boolean isMetricPathFilter()
	{
		return m_IsMetricPathFilter;
	}
	
	/**
	 * True if a metric path like expression was parsed.
	 * @return
	 */
	public boolean isMetricPathLikeFilter()
	{
		return m_IsMetricPathLikeFilter;
	}
	
	/**
	 * True if a escape character was set.
	 * @return
	 */
	public boolean isEscapeCharFilter()
	{
		return m_IsEscapeCharFilter;
	}
	
	
	/**
	 * The anomaly score threshold value.
	 * @return
	 */
	public int getThresholdValue()
	{
		return m_ThresholdValue;
	}
	
	/**
	 * The list of metric path filters anded together.
	 * @return
	 */
	public List<String> getMetricPathAnds()
	{
		return m_MetricPathAnds;
	}
	
	
	/**
	 * The SQL like escape character.
	 * @return
	 */
	public String getEscapeCharValue()
	{
		return m_EscapeCharValue;
	}
	
	
	/**
	 * The list of metric path like filters anded together.
	 * @return
	 */
	public List<String> getMetricPathLikeAnds()
	{
		return m_MetricPathLikeAnds;
	}
	
	/**
	 * The value of the min first evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMinFirstEvidenceTime()
	{
		return m_MinFirstEvidenceTime;
	}
	
	/**
	 * The value of the max first evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMaxFirstEvidenceTime()
	{
		return m_MaxFirstEvidenceTime;
	}
	
	/**
	 * The value of the min last evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMinLastEvidenceTime()
	{
		return m_MinLastEvidenceTime;
	}
	
	/**
	 * The value of the max last evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMaxLastEvidenceTime()
	{
		return m_MaxLastEvidenceTime;
	}
	
	/**
	 * The value of the min peak evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMinPeakEvidenceTime()
	{
		return m_MinPeakEvidenceTime;
	}
	
	/**
	 * The value of the max peak evidence time filter.
	 * @return Value will be <code>null</code> if no filter was parsed.
	 */
	public DateTimeFilter getMaxPeakEvidenceTime()
	{
		return m_MaxPeakEvidenceTime;
	}

	/**
	 * The value of the min update time filter.
	 * @return Value will be <code>null</code> if no filter was parsed. 
	 */
	public DateTimeFilter getMinUpdateTime()
	{
		return m_MinUpdateTime;
	}
	
	/**
	 * The value of the max update time filter.
	 * @return Value will be <code>null</code> if no filter was parsed. 
	 */
	public DateTimeFilter getMaxUpdateTime()
	{
		return m_MaxUpdateTime;
	}
	
	
	public List<OdataExpressionVisitor> getOredExpressions()
	{
		return m_OrExpressions;
	}
	
	
	public ExpressionType getExpressionType()
	{
		return m_ExpressionType;
	}
	
	
	/**
	 * Merge the settings in the expression visitor <code>other</code> with 
	 * this one and return values in a new <code>OdataExpressionVisitor</code>
	 * instance. The function does modify this instance it creates a new instance.
	 * 
	 * @param other Expression to merge with this one.
	 * @return new OdataExpressionVisitor with the values of <code>this</code> and <code>other</code> 
	 * combined.
	 */
	public OdataExpressionVisitor mergeExpression(OdataExpressionVisitor other)
	{
		OdataExpressionVisitor combined = new OdataExpressionVisitor();
		
		mergeAndedExpressions(this, combined);
		mergeAndedExpressions(other, combined);
		combined.m_ExpressionType = ExpressionType.AND;
		
		combined.m_OrExpressions = Collections.emptyList(); // TODO this is wrong.
		
		return combined;
	}
	
	private void mergeOrdedExpresssions(OdataExpressionVisitor orExp)
	{
		List<OdataExpressionVisitor> ordedExps = new ArrayList<OdataExpressionVisitor>();
		
		for (OdataExpressionVisitor visitor : orExp.m_OrExpressions)
		{
//			if (visitor.m_ExpressionType == ExpressionType.BOOLPAREN)
//			{
//				if (visitor.m_OrExpressions.size() == 1)
//				{
//					OdataExpressionVisitor childVisitor = visitor.m_OrExpressions.get(0);
//
//					if (childVisitor.m_ExpressionType.isComparison())
//					{
//						ordedExps.add(visitor);
//					}
//				}
//			}
			if (visitor.m_ExpressionType.isComparison())
			{
				ordedExps.add(visitor);
			}
			else if (visitor.m_ExpressionType == ExpressionType.AND)
			{
				ordedExps.add(visitor);
			}
			else if (visitor.m_ExpressionType == ExpressionType.OR)
			{
				ordedExps.addAll(visitor.m_OrExpressions);
			}
		}
		
		orExp.m_OrExpressions = ordedExps;
	}

	
	/**
	 * Merge expression values from <code>source</code> into <code>target</code>. 
	 * If <code>source</code> has an expression set it will override the value in target.
	 * @param source
	 * @param target
	 */
	private void mergeAndedExpressions(OdataExpressionVisitor source, OdataExpressionVisitor target)
	{
		if (source.isThresholdFilter())
		{	
			setThreshold(source.getThresholdValue(), target, LogicalOperator.GE);
		}
		if (source.isMetricPathFilter())
		{
			setMetricPathAnds(source.getMetricPathAnds(), target, LogicalOperator.EQ);
		}
		if (source.isEscapeCharFilter())
		{
			setEscapeChar(source.getEscapeCharValue(), target, LogicalOperator.EQ);
		}
		if (source.isMetricPathLikeFilter())
		{
			setMetricPathLikes(source.getMetricPathLikeAnds(), target, LogicalOperator.EQ);
		}

		if (source.getMaxFirstEvidenceTime() != null)
		{
			target.m_MaxFirstEvidenceTime = source.getMaxFirstEvidenceTime();
		}

		if (source.getMinFirstEvidenceTime() != null)
		{
			target.m_MinFirstEvidenceTime = source.getMinFirstEvidenceTime();
		}

		if (source.getMaxLastEvidenceTime() != null)
		{
			target.m_MaxLastEvidenceTime = source.getMaxLastEvidenceTime();
		}

		if (source.getMinLastEvidenceTime() != null)
		{
			target.m_MinLastEvidenceTime = source.getMinLastEvidenceTime();
		}

		if (source.getMaxPeakEvidenceTime() != null)
		{
			target.m_MaxPeakEvidenceTime = source.getMaxPeakEvidenceTime();
		}

		if (source.getMinPeakEvidenceTime() != null)
		{
			target.m_MinPeakEvidenceTime = source.getMinPeakEvidenceTime();
		}

		if (source.getMaxUpdateTime() != null)
		{
			target.m_MaxUpdateTime = source.getMaxUpdateTime();
		}

		if (source.getMinUpdateTime() != null)
		{
			target.m_MinUpdateTime = source.getMinUpdateTime();
		}
		
		target.m_OrExpressions.addAll(source.m_OrExpressions);
		
		// have merged expressions so set null.
		target.m_AndExpressions = Collections.emptyList();
	}
	
	
	/**
	 * Extract the property - value pair from the left and right 
	 * expressions and merge into this.
	 * 
	 * @param op The logical operation to apply (le, ge, etc).
	 */
	private void setExpression(OdataExpressionVisitor lhs, OdataExpressionVisitor rhs, LogicalOperator op)
	{
		OdataExpressionVisitor prop;
		OdataExpressionVisitor value;
		if (lhs.m_ExpressionType == ExpressionType.PROPERTY)
		{
			prop = lhs;
			value = rhs;
		}
		else if (lhs.m_ExpressionType == ExpressionType.LITERAL)
		{
			value = lhs;
			prop = rhs;
		}
		else 
		{
			s_Logger.debug("Cannot set property where no property expression");
			return;
		}


		// Set the property value
		if (THRESHOLD_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			// Threshold may be an int, double or long value
			int threshold = Math.max(Math.max(value.m_IntLiteral, (int)value.m_DoubleLiteral),
					(int)value.m_LongLiteral);
			
			setThreshold(threshold, m_CurrentExpression, op);
		}
		else if (METRIC_PATH_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setMetricPathAnd(value.m_StringLiteral, m_CurrentExpression, op);
		}
		else if (PEAK_EVIDENCE_DATE_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setDateTime(value.m_DateLiteral, m_CurrentExpression, op, DateFilter.PEAK_EVIDENCE);
		}
		else if (FIRST_EVIDENCE_DATE_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setDateTime(value.m_DateLiteral, m_CurrentExpression, op, DateFilter.FIRST_EVIDENCE);
		}
		else if (LAST_EVIDENCE_DATE_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setDateTime(value.m_DateLiteral, m_CurrentExpression, op, DateFilter.LAST_EVIDENCE);
		}
		else if (UPDATE_DATE_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setDateTime(value.m_DateLiteral, m_CurrentExpression, op, DateFilter.UPDATE);
		}
		else if (METRIC_PATH_LIKE_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setMetricPathLike(value.m_StringLiteral, m_CurrentExpression, op);
		}
		else if (ESCAPE_CHAR_FILTER.equalsIgnoreCase(prop.m_PropertyName))
		{
			setEscapeChar(value.m_StringLiteral, m_CurrentExpression, op);
		}
		else 
		{
			throw new UnsupportedOperationException(
					String.format("Unknow property name '%s' in expression. Operator = %s", 
							prop.m_PropertyName, op));	
		}
	}
	
	
	/**
	 * Set the anomaly threshold according to the logical operator. 
	 * 
	 * @param value
	 * @param op Must be either LogicalOperator.GE or LogicalOperator.GT
	 */
	private void setThreshold(int value, OdataExpressionVisitor target, LogicalOperator op)
	{
		if (op == LogicalOperator.GE)
		{
			target.m_IsThresholdFilter = true;
			target.m_ThresholdValue = value;
		}
		else if (op == LogicalOperator.GT)
		{
			target.m_IsThresholdFilter = true;
			target.m_ThresholdValue = value +1;
		} 
		else
		{
			throw new UnsupportedOperationException("The operator " + op + " cannot be applied to " + THRESHOLD_FILTER);
		}
	}
	
	
	/**
	 * Set the metric path filter. Only the equality operator can 
	 * be applied to the metric path. 
	 *  
	 * @param value
	 * @param op Must be LogicalOperator.EQ
	 */
	private void setMetricPathAnds(List<String> values, OdataExpressionVisitor target, 
			LogicalOperator op)
	{
		if (op == LogicalOperator.EQ)
		{
			target.m_IsMetricPathFilter = true;
			target.m_MetricPathAnds.addAll(values);
		}
		else
		{
			throw new UnsupportedOperationException("The operator " + op + " cannot be applied to " + METRIC_PATH_FILTER);
		}
	}
	
	private void setMetricPathAnd(String value, OdataExpressionVisitor target, 
			LogicalOperator op)
	{
		if (op == LogicalOperator.EQ)
		{
			target.m_IsMetricPathFilter = true;
			target.m_MetricPathAnds.add(value);
		}
		else
		{
			throw new UnsupportedOperationException("The operator " + op + " cannot be applied to " + METRIC_PATH_FILTER);
		}
	}
	
	
	/**
	 * Set the date filter according to the logical operator. 
	 * 
	 * If op is GE or GT then the earliest date is set, else if the 
	 * op is LE or LT then the latest date is set. If op is EQ then both 
	 * earliest and latest dates are set to the same time.
	 * 
	 * @param value
	 * @param op Must be either LogicalOperator.GE, LogicalOperator.GT,
	 * LogicalOperator.LE, LogicalOperator.LT, LogicalOperator.EQ
	 * @param dateFilter Which date filter is to be set
	 */
	private void setDateTime(Date value, OdataExpressionVisitor target, 
			LogicalOperator op, DateFilter dateFilter)
	{
		if (dateFilter == DateFilter.FIRST_EVIDENCE)
		{
			if (op.isLess())
			{
				target.m_MaxFirstEvidenceTime = new DateTimeFilter(value, op);
			}
			else if (op == LogicalOperator.EQ)
			{
				target.m_MaxFirstEvidenceTime = new DateTimeFilter(value, op);
				target.m_MinFirstEvidenceTime = new DateTimeFilter(value, op);
			}
			else
			{
				target.m_MinFirstEvidenceTime = new DateTimeFilter(value, op);
			}
		}
		else if (dateFilter == DateFilter.LAST_EVIDENCE)
		{
			if (op.isLess())
			{
				target.m_MaxLastEvidenceTime = new DateTimeFilter(value, op);
			}
			else if (op == LogicalOperator.EQ)
			{
				target.m_MinLastEvidenceTime = new DateTimeFilter(value, op);
				target.m_MaxLastEvidenceTime = new DateTimeFilter(value, op);
			}
			else
			{
				target.m_MinLastEvidenceTime = new DateTimeFilter(value, op);
			}
		} 
		else if (dateFilter == DateFilter.PEAK_EVIDENCE)
		{
			if (op.isLess())
			{
				target.m_MaxPeakEvidenceTime = new DateTimeFilter(value, op);
			}
			else if (op == LogicalOperator.EQ)
			{
				target.m_MinPeakEvidenceTime = new DateTimeFilter(value, op);
				target.m_MaxPeakEvidenceTime = new DateTimeFilter(value, op);
			}
			else
			{
				target.m_MinPeakEvidenceTime = new DateTimeFilter(value, op);
			}
		}
		else if (dateFilter == DateFilter.UPDATE)
		{
			if (op.isLess())
			{
				target.m_MaxUpdateTime = new DateTimeFilter(value, op);
			}
			else if (op == LogicalOperator.EQ)
			{
				target.m_MinUpdateTime = new DateTimeFilter(value, op);
				target.m_MaxUpdateTime = new DateTimeFilter(value, op);
			}
			else
			{
				target.m_MinUpdateTime = new DateTimeFilter(value, op);
			}
		}
		else
		{
			throw new UnsupportedOperationException("The operator " + op + " cannot be applied to " + PEAK_EVIDENCE_DATE_FILTER);
		}
	}

	
	/**
	 * Set the metric path for SQL like queries.
	 * 
	 * @param metricPathLike 
	 * @param op This should be EQ but it is ignored
	 */
	private void setMetricPathLikes(List<String> metricPathLike, OdataExpressionVisitor target, 
			LogicalOperator op)
	{
		target.m_IsMetricPathLikeFilter = true;
		target.m_MetricPathLikeAnds.addAll(metricPathLike);
	}
	
	
	private void setMetricPathLike(String metricPathLike, OdataExpressionVisitor target, 
			LogicalOperator op)
	{
		target.m_IsMetricPathLikeFilter = true;
		target.m_MetricPathLikeAnds.add(metricPathLike);
	}
	
	/**
	 * Set the escape character for the SQL like queries.
	 * 
	 * @param escapeChar 
	 * @param op This should be EQ but it is ignored
	 */
	private void setEscapeChar(String escapeChar, OdataExpressionVisitor target, 
			LogicalOperator op)
	{
		target.m_IsEscapeCharFilter = true;
		target.m_EscapeCharValue = escapeChar;
	}
	
	@Override
	public String toString()
	{
		return this.m_ExpressionType.toString();
	}
	
}
