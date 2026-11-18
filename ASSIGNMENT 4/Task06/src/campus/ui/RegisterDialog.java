package campus.ui;

import campus.model.EventInfo;
import campus.model.Registration;
import campus.service.DuplicateRegistrationException;
import campus.service.RegistrationService;
import campus.service.ValidationException;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/** Modal registration form. Only collects input and shows messages; rules live in RegistrationService. */
public class RegisterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient RegistrationService service;
    private final JTextField idField = new JTextField(14);
    private final JTextField nameField = new JTextField(14);
    private final JTextField emailField = new JTextField(14);
    private final JComboBox<EventInfo> eventBox = new JComboBox<>();
    private final JTextArea messageArea = new JTextArea(3, 28);
    private final JButton registerButton = new JButton("Register");
    private boolean registered;

    public RegisterDialog(Frame owner, RegistrationService service, java.util.List<EventInfo> events) {
        super(owner, "Register Student for Event", true);
        this.service = service;
        events.forEach(eventBox::addItem);
        eventBox.setSelectedIndex(-1);                         // force the user to choose, so "Select an event" can be tested

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("Student ID:")); form.add(idField);
        form.add(new JLabel("Name:"));       form.add(nameField);
        form.add(new JLabel("E-mail:"));     form.add(emailField);
        form.add(new JLabel("Event:"));      form.add(eventBox);

        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        registerButton.addActionListener(e -> submit());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(registerButton); buttons.add(cancel);

        add(form, BorderLayout.NORTH);
        add(messageArea, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(registerButton);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean wasRegistered() { return registered; }

    private void submit() {
        String id = idField.getText(), name = nameField.getText(), email = emailField.getText();
        EventInfo event = (EventInfo) eventBox.getSelectedItem();
        registerButton.setEnabled(false);
        showMessage("Saving...", Color.DARK_GRAY);
        new SwingWorker<Registration, Void>() {
            @Override protected Registration doInBackground() { return service.register(id, name, email, event); }
            @Override protected void done() {
                registerButton.setEnabled(true);
                try {
                    Registration r = get();
                    registered = true;
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            r.studentId() + " registered for " + r.eventName() + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    if (c instanceof ValidationException || c instanceof DuplicateRegistrationException) {
                        showMessage(c.getMessage(), new Color(0xB00020));     // clear, specific message next to the form
                    } else {
                        showMessage("System error: " + c.getMessage(), new Color(0xB00020));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showMessage(String text, Color color) {
        messageArea.setForeground(color);
        messageArea.setText(text);
    }
}
