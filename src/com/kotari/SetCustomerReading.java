package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class SetCustomerReading extends JDialog {
    private static final double RATE_ABOVE_50 = 0.6942;
    private static final double RATE_BELOW_50 = 0.6087;

    private static final double RATE_SERVICE = 14.49;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel dialog_set_reading_name;
    private JLabel dialog_set_reading_last_reading;
    private JTextField dialog_set_reading_current_reading;
    private JTextField dialog_set_reading_service_charge;
    private JLabel dialog_set_reading_below_50;
    private JLabel dialog_set_reading_above_50;
    private JLabel dialog_set_reading_total_payment;
    private JLabel dialog_set_reading_result;

    private boolean is_reading_customer_info = false;

    private int mCurrentReadingId;
    private int mCustomerId;

    public interface CustomerReadingListener {
        void readingSet();
        void cancelSelected();
    }

    public CustomerReadingListener mListener;

    public void setListener(CustomerReadingListener listener) {
        mListener = listener;
    }

    void setOkBtnStatus() {
        if (is_reading_customer_info) {
            buttonOK.setEnabled(false);
            return;
        }
        buttonOK.setEnabled(
                !dialog_set_reading_current_reading.getText().
                        trim().isEmpty() &&
                        !dialog_set_reading_service_charge.getText().
                                trim().isEmpty());
    }

    double try_double(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    int try_int(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    int getCurrentReading() {
        return try_int(dialog_set_reading_current_reading.
                getText().trim());
    }

    int getPreviousReading() {
        return try_int(dialog_set_reading_last_reading.
                getText().trim());
    }

    double getServiceCharge() {
        return try_double(dialog_set_reading_service_charge.
                getText().trim());
    }

    void updateReadingCalculation() {
        int current_reading = getCurrentReading();
        int previous_reading = getPreviousReading();
        double service = getServiceCharge();

        int difference = current_reading - previous_reading;
        if (difference < 0) difference = 0;

        int above_50 = (difference > 50) ? (difference - 50) : 0;
        int below_50 = (difference < 50) ? difference : 50;

        dialog_set_reading_below_50.setText("" + below_50);
        dialog_set_reading_above_50.setText("" + above_50);

        double payment = below_50 * RATE_BELOW_50 +
                above_50 * RATE_ABOVE_50 + service;
        dialog_set_reading_total_payment.setText("" + format_double(payment));
    }

    public SetCustomerReading() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setMinimumSize(new Dimension(350, 300));

        setTitle("Set Reading");

        dialog_set_reading_service_charge.setEnabled(false);

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dialog_set_reading_result.setVisible(false);
                setOkBtnStatus();
                updateReadingCalculation();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dialog_set_reading_result.setVisible(false);
                setOkBtnStatus();
                updateReadingCalculation();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dialog_set_reading_result.setVisible(false);
                setOkBtnStatus();
                updateReadingCalculation();
            }
        };
        dialog_set_reading_current_reading.getDocument().addDocumentListener(listener);

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

    class ReadingInfo {
        String customer_name;

        int customer_id;
        int tariff_type;

        int current_reading_id;
        int previous_reading_id;

        int initial_reading;
        int previous_reading;

        boolean has_previous_reading;
    }

    public void setReadingValues(final int current_reading_id, final int prev_reading_id, final int customer_id) {
        buttonOK.setEnabled(false);
        is_reading_customer_info = true;

        mCurrentReadingId = current_reading_id;
        mCustomerId = customer_id;

        new SwingWorker<ReadingInfo, Void>() {
            @Override
            protected ReadingInfo doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

                    /**
                     * Query to check the previous reading for the customer.
                     * If not found, use the customer's initial reading as the previous reading
                     */
                    String query = "select " +
                            " c.name, c.initial_reading, c.tariff_type, c_r.reading_id, c_r.current_reading " +
                            " from customer c LEFT JOIN " +
                            "       (select * from reading r " +
                            "           INNER JOIN customer_reading cr " +
                            "           ON r.reading_id = cr.r_id " +
                            "               WHERE r.reading_id = " + prev_reading_id + ") c_r " +
                            " ON (c.customer_id = c_r.c_id) " +
                            "   WHERE c.customer_id = " + customer_id;

                    ResultSet rs = stmt.executeQuery(query);

                    final int COL_INITIAL_READING = 2;
                    final int COL_READING_ID = 4;
                    final int COL_PREVIOUS_READING = 5;

                    String customer_name;

                    int tariff_type;

                    int previous_reading = 0;
                    int initial_reading = 0;

                    boolean previous_exist;

                    if (rs.next()) {
                        customer_name = rs.getString(1);

                        tariff_type = rs.getInt(3);

                        // The previous reading doesn't exist, so use the initial reading instead
                        if (rs.getObject(COL_READING_ID) == null) {
                            previous_exist = false;
                            initial_reading = rs.getInt(COL_INITIAL_READING);
                        } else {
                            previous_exist = true;
                            previous_reading = rs.getInt(COL_PREVIOUS_READING);
                        }
                        rs.close();
                    } else {
                        rs.close();
                        return null;
                    }

                    ReadingInfo info = new ReadingInfo();
                    info.customer_id = customer_id;
                    info.customer_name = customer_name;
                    info.tariff_type = tariff_type;

                    info.previous_reading_id = prev_reading_id;
                    info.current_reading_id = current_reading_id;

                    info.has_previous_reading = previous_exist;
                    info.initial_reading = initial_reading;
                    info.previous_reading = previous_reading;

                    return info;
                } catch (Exception e) {
                    System.err.println("Exception in Load Data" + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ReadingInfo info = get();

                    dialog_set_reading_name.setText(info.customer_name);
                    dialog_set_reading_service_charge.setText("" + Tariffs.getTariffRate(info.tariff_type));

                    String last_reading;
                    if (info.has_previous_reading) {
                        last_reading = "" + info.previous_reading;
                    } else {
                        last_reading = "" + info.initial_reading;
                    }
                    dialog_set_reading_last_reading.setText(last_reading);
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error retrieving " + e.getMessage());
                    dispose();
                }
                is_reading_customer_info = false;
            }
        }.execute();
    }

    private static final DecimalFormat sFormatter;
    static {
        sFormatter = new DecimalFormat("#.####");
    }

    double format_double(double d) {
        return Double.valueOf(sFormatter.format(d));
    }

    private void onOK() {
        final int customer_id = mCustomerId;
        final int reading_id = mCurrentReadingId;

        final int prev_reading = try_int(dialog_set_reading_last_reading.getText().trim());
        final int current_reading = try_int(dialog_set_reading_current_reading.getText().trim());

        final int delta = ((current_reading - prev_reading) >= 0) ?
                (current_reading - prev_reading) : 0;

        final double service = try_double(dialog_set_reading_service_charge.getText().trim());

        final double below = try_double(dialog_set_reading_below_50.getText().trim());
        final double above = try_double(dialog_set_reading_above_50.getText().trim());

        final double total = try_double(dialog_set_reading_total_payment.getText().trim());

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                boolean success;
                String error_msg = null;

                try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
                    PreparedStatement stmt = conn.prepareStatement("insert into customer_reading " +
                            "(r_id, c_id, previous_reading, current_reading, delta_change, below_50, " +
                            " above_50, service_charge, total_payment) values " +
                            " (?, ?, ?, ?, ?, ?, ?, ?, ?) ");
                    stmt.setQueryTimeout(10);       // this is seconds
                    stmt.setInt(1, reading_id);
                    stmt.setInt(2, customer_id);
                    stmt.setInt(3, prev_reading);
                    stmt.setInt(4, current_reading);
                    stmt.setInt(5, delta);

                    stmt.setDouble(6, below);
                    stmt.setDouble(7, above);
                    stmt.setDouble(8, service);

                    stmt.setDouble(9, total);

                    stmt.execute();
                    stmt.close();
                    success = true;
                } catch (SQLException e) {
                    success = false;
                    error_msg = e.getMessage();
                }

                return new Pair<>(success, error_msg);
            }

            @Override
            protected void done() {
                try {
                    Pair<Boolean, String> result = get();
                    if (result.getKey() == true) {      // i.e: success
                        mListener.readingSet();
                        dispose();
                    } else {
                        dialog_set_reading_result.setVisible(true);
                        dialog_set_reading_result.setText(result.getValue());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    dialog_set_reading_result.setVisible(true);
                    dialog_set_reading_result.setText(e.getMessage());
                }
            }
        }.execute();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        SetCustomerReading dialog = new SetCustomerReading();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
