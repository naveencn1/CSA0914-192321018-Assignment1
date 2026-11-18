import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

/**
 * REFERENCE COPY OF THE FLAWED DESIGN (rebuilt from the task description - replace with your supplied file).
 * Used only for the diagnosis in the report. Not part of the redesign.
 */
public class FeePaymentFlawed extends JFrame implements ActionListener {
    JTextField idField = new JTextField(10), amountField = new JTextField(10);
    JButton pay = new JButton("Pay"), receipt = new JButton("Receipt"), clear = new JButton("Clear");
    JLabel status = new JLabel(" ");

    public FeePaymentFlawed() {
        setLayout(new FlowLayout());
        add(new JLabel("Student ID")); add(idField);
        add(new JLabel("Amount"));     add(amountField);
        add(pay); add(receipt); add(clear); add(status);
        pay.setActionCommand("Pay"); receipt.setActionCommand("Receipt"); clear.setActionCommand("Clear");
        pay.addActionListener(this);           // ONE listener for every button
        receipt.addActionListener(this);
        clear.addActionListener(this);
        setSize(300, 200);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == "Pay") {                                            // PROBLEM: == compares references, not text
            try {
                Thread.sleep(3000);                                    // PROBLEM: blocks the EDT - window freezes
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost/college", "root", "root"); // PROBLEM: DB in UI
                Statement st = con.createStatement();
                st.executeUpdate("INSERT INTO payments VALUES('" + idField.getText() + "',"
                        + amountField.getText() + ")");                // PROBLEM: SQL injection, no validation
                status.setText("Paid");                                // PROBLEM: no detail, no feedback on failure
            } catch (Exception ex) {                                   // PROBLEM: swallowed exception
            }                                                          // PROBLEM: Connection / Statement never closed
        } else if (cmd.equals("Receipt")) {
            // PROBLEM: empty - Receipt button does nothing
        } else if (cmd.equals("Clear")) {
            idField.setText(""); amountField.setText("");
        }
    }
}
