package demo.app.client.splash.applet;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import netscape.javascript.JSObject;

public class ChartAppletImpl extends JApplet
{
	private JTextField m_Counter;
	
	private JLabel m_LabelField;


	public void init()
	{
		JPanel panelMain = new JPanel();
		panelMain.setBorder(BorderFactory.createTitledBorder("CounterApplet"));
		panelMain.setBackground(Color.WHITE);
		
		m_Counter = new JTextField(20);
		m_Counter.setHorizontalAlignment(JTextField.CENTER);
		m_Counter.setText("0");
		m_Counter.setEditable(false);
		panelMain.add(new JLabel("Current count : "));
		panelMain.add(m_Counter);
		
		m_LabelField = new JLabel();
		panelMain.add(m_LabelField);
		
		JButton displayBtn = new JButton("Display on page");
		displayBtn.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e)
            {
	            System.out.println("Value is " + getCurrentValue());
	            
	            JSObject window = JSObject.getWindow(ChartAppletImpl.this);
	            window.call("displayValue", new Object[]{23});

            }
			
		});
		panelMain.add(displayBtn);

		getContentPane().add(panelMain);
	}


	public void increment()
	{
		int currentCount = Integer.parseInt(m_Counter.getText());
		currentCount++;
		m_Counter.setText(currentCount + "");
	}


	public void decrement()
	{
		int currentCount = Integer.parseInt(m_Counter.getText());
		currentCount--;
		m_Counter.setText(currentCount + "");
	}


	public Object getCurrentValue()
	{
		return m_Counter.getText();
	}
	
	
	public void setLabelId(String id)
	{
		m_LabelField.setText(id);
	}
}
