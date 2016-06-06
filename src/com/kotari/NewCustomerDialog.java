package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class NewCustomerDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_new_customer_name;
    private JTextField dialog_new_customer_contract_no;
    private JTextField dialog_new_customer_type_of_business;
    private JComboBox dialog_new_customer_tariff_type;
    private JTextField dialog_new_customer_address;
    private JTextField dialog_new_customer_phone;
    private JButton dialog_new_customer_btn_select_meter;
    private JLabel dialog_new_customer_meter_label;
    private JLabel dialog_new_customer_meter_value;

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

        Vector<Tariffs.TariffInfo> tariffs = new Vector<>();
        for (int i = 0; i < Tariffs.TARIFF_ARRAY.length; i++) {
            tariffs.add(Tariffs.TARIFF_ARRAY[i]);
        }
        dialog_new_customer_tariff_type.setModel(new DefaultComboBoxModel<Tariffs.TariffInfo>(tariffs));

        dialog_new_customer_tariff_type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        dialog_new_customer_btn_select_meter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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

    void configureForEditing() {
        if (!mIsEditing) return;

        dialog_new_customer_name.setEnabled(false);
        new SwingWorker<CustomerInfo, Void>() {
            @Override
            protected CustomerInfo doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                    CustomerInfo info = new CustomerInfo();
                    ResultSet rs = stmt.executeQuery("select " +
                            " name, address, is_active, phone_number, contract_no, type_of_business, " +
                            " tariff_type, customer_meter_id " +
                            " from customer " +
                            " where customer_id = " + mCustomerId);
                    if (rs.next()) {
                        info.name = rs.getString(1);
                        info.address = rs.getString(2);
                        info.is_active = rs.getInt(3) == DbUtil.D_TRUE;
                        info.phone = rs.getString(4);
                        info.contract_number = rs.getString(5);
                        info.type_of_biz = rs.getString(6);
                        info.tariff_type = rs.getInt(7);
                        info.meter_id = rs.getInt(8);
                        return info;
                    }
                } catch (SQLException e) {
                    System.err.println("Querying customer failed " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    CustomerInfo info = get();
                    if (info == null) {
                        setVisible(false);
                    }

                    dialog_new_customer_name.setText(info.name);
                    dialog_new_customer_address.setText(info.address);
                    dialog_new_customer_phone.setText(info.phone);
                    dialog_new_customer_contract_no.setText(info.contract_number);
                    dialog_new_customer_type_of_business.setText(info.type_of_biz);
                    dialog_new_customer_tariff_type.setSelectedIndex(info.tariff_type);
                    // TODO: set meter id by some means
                } catch (ExecutionException | InterruptedException e) {

                }
            }
        }.execute();
    }

    void setOkBtnStatus() {
        boolean enable = !dialog_new_customer_name.getText().trim().isEmpty();
        buttonOK.setEnabled(enable);
    }

    class CustomerInfo {
        String name;
        String address;
        boolean is_active;
        String phone;
        String contract_number;
        String type_of_biz;
        int tariff_type;
        int meter_id;
    }

    void setEditStatus(boolean is_editing, int customer_id) {
        mIsEditing = is_editing;
        mCustomerId = customer_id;
        configureForEditing();
    }

    Pair<Boolean, String> createCustomer(Customer customer) {
        boolean success = false;
        String error_msg = null;

        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement("insert into customer " +
                    "(name, address, is_active, phone_number, contract_no, type_of_business, " +
                    "   tariff_type, customer_meter_id) values " +
                    " (?, ?, ?, ?, ?, ?, ?, ?) ");
            stmt.setQueryTimeout(10);       // this is seconds
            stmt.setString(1, customer.name);
            stmt.setString(2, customer.address);
            stmt.setInt(3, DbUtil.D_TRUE);
            stmt.setString(4, customer.phone);
            stmt.setString(5, customer.contract_no);
            stmt.setString(6, customer.type_of_business);
            stmt.setInt(7, customer.tariff_type);
            stmt.setInt(8, customer.meter_id);

            stmt.execute();
            success = true;
        } catch (SQLException e) {
            success = false;
            error_msg = e.getMessage();
        }

        return new Pair<>(success, error_msg);
    }

    Pair<Boolean, String> updateCustomer(Customer customer, int customer_id) {
        boolean success = false;
        String error_msg = null;

        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement("update customer set " +
                    "name = ?, address = ?, is_active = ?, phone_number = ?, contract_no = ?, " +
                    "type_of_business = ?, tariff_type = ?, customer_meter_id = ? " +
                    " where customer_id = " + customer_id);
            stmt.setQueryTimeout(10);       // this is seconds
            stmt.setString(1, customer.name);
            stmt.setString(2, customer.address);
            stmt.setInt(3, (customer.is_active ? DbUtil.D_TRUE : DbUtil.D_FALSE));
            stmt.setString(4, customer.phone);
            stmt.setString(5, customer.contract_no);
            stmt.setString(6, customer.type_of_business);
            stmt.setInt(7, customer.tariff_type);
            stmt.setInt(8, customer.meter_id);

            success = stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            success = false;
            error_msg = e.getMessage();
        }

        return new Pair<>(success, error_msg);
    }

    private void onOK() {
        Customer customer = new Customer();
        customer.name = dialog_new_customer_name.getText().trim();
        customer.address = dialog_new_customer_address.getText().trim();
        customer.phone = dialog_new_customer_phone.getText().trim();
        customer.contract_no = dialog_new_customer_contract_no.getText().trim();
        customer.type_of_business = dialog_new_customer_type_of_business.getText().trim();
        customer.tariff_type = dialog_new_customer_tariff_type.getSelectedIndex();
        customer.is_active = true;

        /*
        // TODO: copy this stuff for the meter dialog
        try {
            customer.initial_reading = Integer.parseInt(dialog_new_customer_current_reading.getText().trim());
        } catch (NumberFormatException e) {
        }
        */

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                if (mIsEditing) {
                    return updateCustomer(customer, mCustomerId);
                } else {
                    return createCustomer(customer);
                }
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
                        //dialog_new_customer_result.setText(result.getValue());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    if (mIsEditing)
                        System.err.println("Error Updating customer " + e.getMessage());
                    else
                        System.err.println("Error creating customer " + e.getMessage());
                    //dialog_new_customer_result.setText(e.getMessage());
                }
            }
        }.execute();
    }

    private void onCancel() {
        mListener.cancelSelected();
        dispose();
    }

    private boolean mIsEditing = false;
    private int mCustomerId = -1;

    public static void main(String[] args) {
        NewCustomerDialog dialog = new NewCustomerDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
