package com.kotari;

import com.sun.deploy.util.StringUtils;
import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

/**
 * Created by fuad on 5/2/16.
 */
public class KotariUI extends JFrame {
    public KotariUI() {
        setTitle("Kotari");
        setMinimumSize(new Dimension(500, 500));

        setLocationRelativeTo(null);
        setContentPane(kotariMainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        btnPrevPeriod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected_reading_index--;
                updateMeterReadingStatus();
                updateCustomerListTable();
            }
        });
        nextReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected_reading_index++;
                updateMeterReadingStatus();
                updateCustomerListTable();
            }
        });
        setReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                updateCustomerListTable();
            }
        });
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        addCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCustomerDialog dialog = new NewCustomerDialog();
                dialog.setListener(new NewCustomerDialog.CustomerListener() {
                    @Override
                    public void customerAdded() {
                        updateCustomerListTable();
                    }

                    @Override
                    public void cancelSelected() {
                    }
                });
                dialog.setLocationRelativeTo(addCustomerButton);
                dialog.setVisible(true);
            }
        });
        deleteCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        newReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewReadingDialog dialog = new NewReadingDialog();
                dialog.setLocationRelativeTo(newReadingButton);
                dialog.setListener(new NewReadingDialog.ReadingListener() {
                    @Override
                    public void readingAdded() {
                        updateMeterReadingStatus();
                    }

                    @Override
                    public void cancelSelected() {
                    }
                });
                dialog.setVisible(true);
            }
        });

        customerListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                setActionButtonStatus();
            }
        });
        setActionButtonStatus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                updateMeterReadingStatus();
                updateCustomerListTable();
            }
        });
    }

    void setActionButtonStatus() {
        int[] selected_rows = customerListTable.getSelectedRows();
        boolean selected = false;
        if (selected_rows.length > 0) {
            selected = true;
        }

        setReadingButton.setEnabled(selected &&
                current_reading_id != -1 &&
                reading_id_names != null &&
                !reading_id_names.isEmpty());
        infoButton.setEnabled(selected);
        historyButton.setEnabled(selected);
        printButton.setEnabled(selected);
    }

    private static String[][] columnsToShow = {
            // from the customer table
            //{"Customer Name", "c.name"},
            {"Name", "c.name"},
            {"Floor", "c.floor"},
            {"Shop", "c.shop_location"},
            //{"Business Type", "c.type_of_business"},

            // from the reading table
            {"Reading Date", "c_r.date"},

            // from the customer_reading table
            {"Previous", "c_r.previous_reading"},
            {"Current", "c_r.current_reading"},
            {"Below 50kW", "c_r.below_50"},
            {"Above 50kW", "c_r.above_50"},
            //{"Service Charge", "c_r.service_charge"},
            {"Total Payment", "c_r.total_payment"}
    };

    /**
     * Indexes into the {@code columnsToShow} table
     */
    private Vector<String> selectColumnIndex(int index) {
        if (index > 1) index = 1;
        Vector<String> result = new Vector<>();
        for (int i = 0; i < columnsToShow.length; i++) {
            result.add(columnsToShow[i][index]);
        }
        return result;
    }

    void updateCustomerListTable() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                tableModel = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) { return false; }
                };

                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

                    String columns = StringUtils.join(selectColumnIndex(1), ",");
                    String query = "" +
                            "select " + columns + " from customer c LEFT JOIN " +
                            "       (select * from reading r " +
                            "           INNER JOIN customer_reading cr " +
                            "           ON r.reading_id = cr.r_id) c_r " +
                            " ON (c.customer_id = c_r.c_id) ";
                    if (current_reading_id != -1) {
                        query += " where c_r.reading_id is NULL or c_r.reading_id = " + current_reading_id + " ";
                    }

                    query += " order by c.customer_id asc";

                    ResultSet rs = stmt.executeQuery(query);

                    Vector<String> columnNames = selectColumnIndex(0);
                    System.out.println("Columns: " + columnNames);

                    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
                    while (rs.next()) {
                        Vector<Object> vector = new Vector<Object>();
                        for (int i = 1; i <= columnNames.size(); i++) {
                            vector.add(rs.getObject(i));
                        }
                        data.add(vector);
                    }

                    tableModel.setDataVector(data, columnNames);

                    updateMeterReadingStatus();
                } catch (Exception e) {
                    System.err.println("Exception in Load Data" + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                customerListTable.setModel(tableModel);
            }
        }.execute();
    }

    void updateMeterReadingStatus() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DriverManager.
                        getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("select reading_id, date from reading order by reading_id asc;");

                    int previous_size = (reading_id_names != null) ? reading_id_names.size() : 0;
                    reading_id_names = new Vector<Pair<Integer, String>>();

                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String date = rs.getString(2);

                        reading_id_names.add(new Pair<>(id, date));
                    }

                    if (reading_id_names.isEmpty()) {
                        reading_id_names = null;
                        current_reading_id = -1;
                        selected_reading_index = -1;
                    } else {
                        // its our first time searching for ids
                        // OR
                        // there is a newly added reading, make it the current reading
                        if (current_reading_id == -1 ||
                                selected_reading_index == -1 ||
                                (previous_size != reading_id_names.size())) {
                            selected_reading_index = reading_id_names.size() - 1;
                            current_reading_id = reading_id_names.get(selected_reading_index).getKey();
                        }
                    }
                } catch (Exception e) {
                    reading_id_names = null;
                    current_reading_id = -1;
                    selected_reading_index = -1;

                    System.err.println("Exception in Load Reading Data" + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                if (selected_reading_index == -1) {
                    textReadingPeriod.setText("No Reading Period Selected");
                } else {
                    Pair<Integer, String> current = reading_id_names.get(selected_reading_index);
                    textReadingPeriod.setText(current.getValue());
                }

                boolean enable_prev, enable_next;
                if (reading_id_names == null || reading_id_names.isEmpty()) {
                    enable_next = enable_prev = false;
                } else {
                    if (selected_reading_index == 0) {
                        enable_prev = false;
                    } else {
                        enable_prev = true;
                    }
                    if (selected_reading_index == (reading_id_names.size() - 1)) {
                        enable_next = false;
                    } else {
                        enable_next = true;
                    }
                }

                nextReadingButton.setEnabled(enable_next);
                btnPrevPeriod.setEnabled(enable_prev);
            }
        }.execute();
    }

    public static void main(String[] args) {
        if (!DbUtil.initDatabase()) {
            System.exit(1);
        }
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new KotariUI().setVisible(true);
            }
        });
    }

    private Vector<Pair<Integer, String>> reading_id_names = null;
    private int selected_reading_index = -1;
    private int current_reading_id = -1;

    private DefaultTableModel tableModel;
    private JPanel kotariMainPanel;
    private JLabel textFieldCompanyTitle;
    private JButton btnPrevPeriod;
    private JButton nextReadingButton;
    private JTable customerListTable;
    private JButton infoButton;
    private JButton historyButton;
    private JButton printButton;
    private JButton setReadingButton;
    private JButton addCustomerButton;
    private JButton deleteCustomerButton;
    private JButton newReadingButton;
    private JLabel textReadingPeriod;
}
