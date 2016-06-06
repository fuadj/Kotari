package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.concurrent.ExecutionException;

public class NewMeterDialog extends JDialog {
    private DialogOperationListener mListener;
    public void setListener(DialogOperationListener listener) { mListener = listener; }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dialog_new_meter_property_number;
    private JTextField dialog_new_meter_initial_reading;
    private JTextField dialog_new_meter_date_of_install;
    private JTextField dialog_new_meter_floor;
    private JTextField dialog_new_meter_shop;

    public NewMeterDialog() {
        setContentPane(contentPane);
        setModal(true);
        setTitle("New Meter Setup");
        setMinimumSize(new Dimension(450, 300));
        getRootPane().setDefaultButton(buttonOK);

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

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    Pair<Boolean, String> createMeter(Meter meter) {
        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement("insert into meter " +
                    "(property_number, initial_reading, date_of_install, floor, shop) values " +
                    " (?, ?, ?, ?, ?) ");

            stmt.setQueryTimeout(5);
            stmt.setString(1, meter.meter_property_number);
            stmt.setInt(2, meter.initial_reading);
            stmt.setString(3, meter.date_of_install);
            stmt.setString(4, meter.floor);
            stmt.setString(5, meter.shop);
            stmt.execute();
            return new Pair<>(Boolean.TRUE, null);
        } catch (SQLException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }

    Pair<Boolean, String> updateMeter(Meter meter) {
        try (Connection conn = DriverManager.getConnection(DbUtil.connection_string)) {
            PreparedStatement stmt = conn.prepareStatement("update meter set " +
                    "initial_reading = ?, date_of_install = ?, floor = ?, shop = ? " +
                    " where meter_id = " + meter.meter_id);
            stmt.setQueryTimeout(5);
            stmt.setInt(1, meter.initial_reading);
            stmt.setString(2, meter.date_of_install);
            stmt.setString(3, meter.floor);
            stmt.setString(4, meter.shop);

            return new Pair<>(stmt.executeUpdate() == 1, null);
        } catch (SQLException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }

    String grab_text(JTextField t) {
        return t.getText().trim();
    }

    private void onOK() {
        Meter meter = new Meter();
        // this is only used in the update
        meter.meter_id = mMeterId;

        meter.meter_property_number = grab_text(dialog_new_meter_property_number);
        meter.initial_reading = CommonUtils.try_int(grab_text(dialog_new_meter_initial_reading));
        meter.date_of_install = grab_text(dialog_new_meter_date_of_install);
        meter.floor = grab_text(dialog_new_meter_floor);
        meter.shop = grab_text(dialog_new_meter_shop);

        new SwingWorker<Pair<Boolean, String>, Void>() {
            @Override
            protected Pair<Boolean, String> doInBackground() throws Exception {
                if (mIsEditing) {
                    return updateMeter(meter);
                } else {
                    return createMeter(meter);
                }
            }

            @Override
            protected void done() {
                try {
                    Pair<Boolean, String> result = get();
                    if (result.getKey() == true) {
                        mListener.operationFinished();
                        dispose();
                    }
                    mListener.operationCanceled();
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        }.execute();
    }

    private void onCancel() {
        mListener.operationCanceled();
        dispose();
    }

    public void setMeterDialogArguments(boolean is_editing, int meter_id) {
        mIsEditing = is_editing;
        mMeterId = meter_id;
        configureForEditing();
    }

    private void configureForEditing() {
        if (!mIsEditing) return;

        dialog_new_meter_property_number.setEnabled(false);
        new SwingWorker<Meter, Void>() {
            @Override
            protected Meter doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                    Meter info = new Meter();
                    ResultSet rs = stmt.executeQuery("select " +
                            " property_number, initial_reading, date_of_install, floor, shop, name " +
                            " from meter left join customer on meter.meter_id = customer.customer_meter_id " +
                            " where meter_id = " + mMeterId);
                    if (rs.next()) {
                        info.meter_id = mMeterId;
                        info.meter_property_number = rs.getString(1);
                        info.initial_reading = rs.getInt(2);
                        info.date_of_install = rs.getString(3);
                        info.floor = rs.getString(4);
                        info.shop = rs.getString(5);
                        info.current_customer_name = rs.getString(6);
                        return info;
                    }
                } catch (SQLException e) {
                    System.err.println("Querying Meter error " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Meter info = get();
                    dialog_new_meter_property_number.setText(info.meter_property_number);
                    dialog_new_meter_initial_reading.setText("" + info.initial_reading);
                    dialog_new_meter_date_of_install.setText(info.date_of_install);
                    dialog_new_meter_floor.setText(info.floor);
                    dialog_new_meter_shop.setText(info.shop);
                } catch (ExecutionException | InterruptedException e) {
                }
            }
        }.execute();
    }

    void setOkBtnStatus() {
        buttonOK.setEnabled(!dialog_new_meter_property_number.getText().trim().isEmpty());
    }

    private boolean mIsEditing = false;
    private int mMeterId = DbUtil.DEFAULT_METER_ID;

    public static void main(String[] args) {
        NewMeterDialog dialog = new NewMeterDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
