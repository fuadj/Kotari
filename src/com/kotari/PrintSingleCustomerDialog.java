package com.kotari;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.sql.*;
import java.text.MessageFormat;
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
            String _header = (Company.getSingleton().name == null ||
                    (Company.getSingleton().name.isEmpty())) ?
                    "Electric Bill" :
                    (Company.getSingleton().name + " Electric Bill");
            if (mAlreadyPrinted)
                _header += " [DUPLICATE]";
            MessageFormat header = new MessageFormat(_header);
            MessageFormat footer = new MessageFormat("Page{0,number,integer}");
            print_single_dialog_customer_table.print(JTable.PrintMode.FIT_WIDTH, header, footer,
                    true, null, true, null);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (Connection conn = DriverManager.
                            getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                        stmt.execute("update meter_reading set reading_printed = " + DbUtil.D_TRUE +
                                " where c_id = " + mCustomerId + " AND " +
                                " r_id = " + mReadingId);
                    } catch (SQLException e) {
                    }
                    return null;
                }

                @Override
                protected void done() {
                    dispose();
                }
            }.execute();
        } catch (PrinterException msg) {
            System.err.println("Error in printing " + msg.getMessage());
            dispose();
        }
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public void setPrintingValues(int customer_id, int reading_id) {
        buttonOK.setEnabled(false);

        mCustomerId = customer_id;
        mReadingId = reading_id;

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
                    final String COL_READING_ID = "m_r.reading_id";
                    final String COL_ALREADY_PRINTED = "m_r.reading_printed";
                    String [][]_columns = {
                            {"Name", "c.name"},
                            {"Floor", "m.floor"},
                            {"Shop", "m.shop"},

                            {COL_READING_ID, COL_READING_ID},
                            {COL_ALREADY_PRINTED, COL_ALREADY_PRINTED},

                            {"Date", "m_r.date"},

                            {"Previous Reading", "m_r.previous_reading"},
                            {"Current Reading", "m_r.current_reading"},

                            {"Month Usage", "m_r.delta_change"},

                            {"Service Charge", "m_r.service_charge"},
                            {"Total Payment", "m_r.total_payment"},
                    };

                    Vector<String> query_columns = new Vector<>();
                    for (int i = 0; i < _columns.length; i++) {
                        query_columns.add(_columns[i][1]);
                    }
                    String query = "" +
                            "select " + CustomStringUtil.join(query_columns, ", ") +
                            " from customer c " +
                                    " LEFT JOIN " +
                                    " (select * from reading r INNER JOIN meter_reading mr " +
                                    "       ON r.reading_id = mr.r_id " +
                                    "       where r.reading_id = " + reading_id + " ) m_r " +
                                    "   ON c.customer_id = m_r.c_id " +
                                    "" +
                                    " LEFT JOIN " +
                                    " (select * from meter " +
                                    "       where meter_id != " + DbUtil.DEFAULT_METER_ID + ") m " +
                                    " ON m.meter_id = c.customer_meter_id " +
                                    " where c.customer_id = " + customer_id;
                    ResultSet rs = stmt.executeQuery(query);

                    Vector<String> columnNames = new Vector<>();
                    for (int i = 0; i < _columns.length; i++) {
                        if (_columns[i][0].equals(COL_READING_ID) ||
                                _columns[i][0].equals(COL_ALREADY_PRINTED)) continue;

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
                            } else if (query_columns.get(i-1).equals(COL_ALREADY_PRINTED)) {
                                if (mDataRead && (obj != null)) {
                                    mAlreadyPrinted = ((Integer)obj) != 0;
                                }
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
    private boolean mAlreadyPrinted = false;

    public static void main(String[] args) {
        PrintSingleCustomerDialog dialog = new PrintSingleCustomerDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
