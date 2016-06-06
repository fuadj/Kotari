package com.kotari;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.sql.*;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class PrintSingleCustomerDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable print_single_dialog_customer_table;

    public PrintSingleCustomerDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setMinimumSize(new Dimension(500, 300));
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
        try {
            print_single_dialog_customer_table.print(JTable.PrintMode.FIT_WIDTH, null, null);
        } catch (PrinterException msg) {
            System.err.println("Error in printing " + msg.getMessage());
        }
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public void setPrintingValues(int customer_id, int reading_id) {
        buttonOK.setEnabled(false);

        mDataRead = false;
        mTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                    final String COL_READING_ID = "c_r.reading_id";
                    String [][]_columns = {
                            {"Name", "c.name"},
                            {"Floor", "c.floor"},
                            {"Shop", "c.shop_location"},

                            {COL_READING_ID, COL_READING_ID},
                            {"Date", "c_r.date"},

                            {"Previous Reading", "c_r.previous_reading"},
                            {"Current Reading", "c_r.current_reading"},

                            {"Month Usage", "c_r.delta_change"},

                            {"Service Charge", "c_r.service_charge"},
                            {"Total Payment", "c_r.total_payment"},
                    };

                    Vector<String> query_columns = new Vector<>();
                    for (int i = 0; i < _columns.length; i++) {
                        query_columns.add(_columns[i][1]);
                    }
                    String query = "select " + CustomStringUtil.join(query_columns, ", ") +
                            " from customer c LEFT JOIN " +
                            "       (select * from reading r " +
                            "           INNER JOIN customer_reading cr " +
                            "           ON r.reading_id = cr.r_id " +
                            "               WHERE r.reading_id = " + reading_id + ") c_r " +
                            " ON (c.customer_id = c_r.c_id) " +
                            "   WHERE c.customer_id = " + customer_id;
                    ResultSet rs = stmt.executeQuery(query);

                    Vector<String> columnNames = new Vector<>();
                    for (int i = 0; i < _columns.length; i++) {
                        if (_columns[i][0].equals(COL_READING_ID)) continue;

                        columnNames.add(_columns[i][0]);
                    }

                    Vector<Vector<Object>> data = new Vector<>();
                    if (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        mCustomerName = rs.getString(1);
                        for (int i = 1; i <= query_columns.size(); i++) {
                            Object obj = rs.getObject(i);
                            if (query_columns.get(i-1).equals(COL_READING_ID)) {
                                if (obj != null)
                                    mDataRead = true;
                            } else {
                                row.add(obj);
                            }
                        }
                        data.add(row);
                        mTableModel.setDataVector(data, columnNames);
                    }
                } catch (SQLException e) {
                    System.out.println("Error Retrieving Customer info " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                print_single_dialog_customer_table.setModel(mTableModel);
                setTitle("Print " + mCustomerName + "'s bill");
                buttonOK.setEnabled(mDataRead);
            }
        }.execute();
    }

    private DefaultTableModel mTableModel;

    private int mCustomerId;
    private int mReadingId;
    private boolean mDataRead;
    private String mCustomerName;

    public static void main(String[] args) {
        PrintSingleCustomerDialog dialog = new PrintSingleCustomerDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
