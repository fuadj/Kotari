package com.kotari;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class MeterHistory extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTable dialog_history_table;

    public MeterHistory() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonCancel);
        setMinimumSize(new Dimension(400, 600));
        setTitle("Meter History");

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

    private void onCancel() {
        dispose();
    }

    public void setMeterId(int meter_id) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                tableModel = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

                    String query = "" +
                            "select name, address, is_active, " +
                            "date, reading_printed, previous_reading, current_reading, " +
                            "total_payment FROM " +
                            "meter INNER JOIN meter_reading ON meter_id = m_id " +
                            " INNER JOIN reading ON r_id = reading_id " +
                            " INNER JOIN customer ON c_id = customer_id " +
                            " WHERE meter_id = " + meter_id;

                    ResultSet rs = stmt.executeQuery(query);

                    Vector<String> columnNames = new Vector<>();

                    columnNames.add("Customer Name");
                    columnNames.add("Address");
                    columnNames.add("Is Working");
                    columnNames.add("Date");
                    columnNames.add("Printed Report");
                    columnNames.add("Previous");
                    columnNames.add("Current");
                    columnNames.add("Payment");

                    Vector<Vector<Object>> data = new Vector<>();
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        Meter info = new Meter();
                        for (int i = 1; i <= 8; i++) {
                            Object obj = rs.getObject(i);
                            switch (i) {
                                case 1:
                                case 2:
                                case 4:
                                    row.add(obj);
                                    break;
                                case 6:
                                case 7:
                                    row.add(obj.toString() + "kw");
                                    break;
                                case 3:
                                case 5:
                                {
                                    boolean working = ((Integer) obj).intValue() != 0;
                                    row.add(working ? "Yes" : "No");
                                    break;
                                }
                                case 8:
                                default:
                                    row.add(obj.toString() + " birr");
                            }
                        }
                        data.add(row);
                    }

                    tableModel.setDataVector(data, columnNames);
                } catch (SQLException e) {
                    System.err.println("Exception in load meter data: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                if (tableModel != null) {
                    dialog_history_table.setModel(tableModel);
                }
            }
        }.execute();
    }

    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        MeterHistory dialog = new MeterHistory();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
