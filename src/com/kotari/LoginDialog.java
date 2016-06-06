package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.concurrent.ExecutionException;

public class LoginDialog extends JDialog {
    private DialogOperationListener mListener;
    public void setListener(DialogOperationListener listener) { mListener = listener; }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_login_name;
    private JPasswordField dialog_login_password;
    private JLabel dialog_login_error;

    public LoginDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Login");
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(300, 200));

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setOkBtnStatus();
            }
        };
        dialog_login_name.getDocument().addDocumentListener(listener);
        dialog_login_password.getDocument().addDocumentListener(listener);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        dialog_login_error.setVisible(false);
        setOkBtnStatus();
    }

    String getPassword() {
        return new String(dialog_login_password.getPassword());
    }

    void setOkBtnStatus() {

        buttonOK.setEnabled(!dialog_login_name.getText().trim().isEmpty() &&
            !getPassword().isEmpty());
    }

    private void onOK() {
        final String username = dialog_login_name.getText().trim();
        final String password = getPassword();
        final String hash = User.hashPassword(password);

        new SwingWorker<Pair<User, String>, Void>() {
            @Override
            protected Pair<User, String> doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string)) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "select id, name, role from users " +
                                    " where name = ? AND pass_hash = ?");
                    stmt.setString(1, username);
                    stmt.setString(2, hash);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        User u = new User();
                        u.id = rs.getInt(1);
                        u.username = rs.getString(2);
                        u.role = rs.getInt(3);
                        return new Pair<>(u, null);
                    } else {
                        return new Pair<>(null, "Invalid username and password, try again");
                    }
                } catch (SQLException e) {
                    return new Pair<>(null, "Error: " + e.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    Pair<User, String> us = get();
                    if (us.getKey() != null) {
                        User u = us.getKey();
                        User.getSingleton().username = u.username;
                        User.getSingleton().id = u.id;
                        User.getSingleton().role = u.role;
                        mListener.operationFinished();
                        dispose();
                    } else {
                        String err_msg = us.getValue();
                        if (err_msg != null) {
                            dialog_login_error.setVisible(true);
                            dialog_login_error.setText(err_msg);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        }.execute();
    }

    private void onCancel() {
        mListener.operationCanceled();
        dispose();
    }

    public static void main(String[] args) {
        LoginDialog dialog = new LoginDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
