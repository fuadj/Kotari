package com.kotari;

import com.kotari.models.Customer;
import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.concurrent.ExecutionException;

public class NewCustomerDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_new_customer_name;
    private JTextField dialog_new_customer_floor;
    private JTextField dialog_new_customer_shop_location;
    private JTextField dialog_new_customer_date_of_install;
    private JTextField dialog_new_customer_current_reading;
    private JTextField dialog_new_customer_contract_no;
    private JTextField dialog_new_customer_type_of_business;
    private JLabel dialog_new_customer_result;

    public interface CustomerListener {
        void customerAdded();
        void cancelSelected();
    }

    public CustomerListener mListener;

    public void setListener(CustomerListener listener) {
        mListener = listener;
    }

    public NewCustomerDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setTitle("New Customer");

        setMinimumSize(new Dimension(450, 300));

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
        setOkBtnStatus();

        dialog_new_customer_name.getDocument().addDocumentListener(listener);
        dialog_new_customer_floor.getDocument().addDocumentListener(listener);

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

    void setOkBtnStatus() {
        boolean enable = !dialog_new_customer_name.getText().trim().isEmpty();
        buttonOK.setEnabled(enable);
    }

    Pair<Boolean, String> createCustomer(Customer customer) {
        boolean success = false;
        String error_msg = null;

        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement("insert into customer " +
                    "(name, floor, shop_location, date_of_install, contract_no, type_of_business, initial_reading) " +
                    " values " +
                    " (?, ?, ?, ?, ?, ?, ?)");
            stmt.setQueryTimeout(10);       // this is seconds
            stmt.setString(1, customer.name);
            stmt.setString(2, customer.floor);
            stmt.setString(3, customer.shop_location);
            stmt.setString(4, customer.date_of_install);
            stmt.setString(5, customer.contract_no);
            stmt.setString(6, customer.type_of_business);
            stmt.setInt(7, customer.initial_reading);

            stmt.execute();
            success = true;
        } catch (SQLException e) {
            success = false;
            error_msg = e.getMessage();
        }

        return new Pair<>(success, error_msg);
    }

    private void onOK() {
        Customer customer = new Customer();
        customer.name = dialog_new_customer_name.getText().trim();
        customer.floor = dialog_new_customer_floor.getText().trim();
        customer.shop_location = dialog_new_customer_shop_location.getText().trim();
        customer.date_of_install = dialog_new_customer_date_of_install.getText().trim();
        customer.contract_no = dialog_new_customer_contract_no.getText().trim();
        customer.type_of_business = dialog_new_customer_type_of_business.getText().trim();

        try {
            customer.initial_reading = Integer.parseInt(dialog_new_customer_current_reading.getText().trim());
        } catch (NumberFormatException e) {
        }

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                return createCustomer(customer);
            }
            @Override
            protected void done() {
                try {
                    Pair<Boolean, String> result = get();
                    if (result.getKey() == true) {      // i.e: success
                        mListener.customerAdded();
                        dispose();
                    } else {
                        // display the error message
                        dialog_new_customer_result.setText(result.getValue());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    dialog_new_customer_result.setText(e.getMessage());
                }
            }
        }.execute();
    }

    private void onCancel() {
        mListener.cancelSelected();
        dispose();
    }

    public static void main(String[] args) {
        NewCustomerDialog dialog = new NewCustomerDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
