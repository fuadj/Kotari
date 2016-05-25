package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class NewReadingDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_new_reading_date;
    private JLabel dialog_new_reading_error;
    public ReadingListener mListener;

    public interface ReadingListener {
        void readingAdded();
        void cancelSelected();
    }

    public void setListener(ReadingListener listener) { mListener = listener; }

    void setOkBtnStatus() {
        buttonOK.setEnabled(!dialog_new_reading_date.getText().trim().isEmpty());
    }

    public NewReadingDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setTitle("New Reading");
        setMinimumSize(new Dimension(300, 75));

        dialog_new_reading_date.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dialog_new_reading_error.setVisible(false);
                setOkBtnStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dialog_new_reading_error.setVisible(false);
                setOkBtnStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dialog_new_reading_error.setVisible(false);
                setOkBtnStatus();
            }
        });

        setOkBtnStatus();

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
    }

    private void onOK() {
        final String date = dialog_new_reading_date.getText().trim();

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
                    PreparedStatement stmt = conn.prepareStatement("insert into reading (date) values (?)");
                    stmt.setQueryTimeout(10);
                    stmt.setString(1, date);
                    stmt.execute();
                    return new Pair<>(true, null);
                } catch (SQLException e) {
                    return new Pair<>(false, e.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    Pair<Boolean, String> result = get();
                    if (result.getKey() == true) {      // i.e: success
                        mListener.readingAdded();
                        dispose();
                    } else {
                        dialog_new_reading_error.setVisible(true);
                        dialog_new_reading_error.setText(result.getValue());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    dialog_new_reading_error.setVisible(true);
                    dialog_new_reading_error.setText(e.getMessage());
                }
                dispose();
            }
        }.execute();
    }

    private void onCancel() {
        mListener.cancelSelected();
        dispose();
    }

    public static void main(String[] args) {
        NewReadingDialog dialog = new NewReadingDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
