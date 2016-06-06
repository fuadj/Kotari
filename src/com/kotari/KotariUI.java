package com.kotari;

import javafx.util.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.sql.*;
import java.text.MessageFormat;
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

        customerListTable.setAutoCreateRowSorter(true);

        btnPrevPeriod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected_reading_index--;
                updateDisplay();
            }
        });
        nextReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected_reading_index++;
                updateDisplay();
            }
        });
        setReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SetCustomerReading dialog = new SetCustomerReading();
                int selected_row = customerListTable.getSelectedRow();
                int model_index = customerListTable.convertRowIndexToModel(selected_row);
                int customer_id = row_to_customer_mapping.get(model_index).customer_id;

                int prev_reading_id = -1;
                if (selected_reading_index > 0) {
                    prev_reading_id = reading_id_names.get(selected_reading_index - 1).getKey();
                }

                dialog.setReadingValues(current_reading_id, prev_reading_id, customer_id);
                dialog.setLocationRelativeTo(customerListTable);
                dialog.setListener(new SetCustomerReading.CustomerReadingListener() {
                    @Override
                    public void readingSet() {
                        updateDisplay();
                    }

                    @Override
                    public void cancelSelected() {
                    }
                });
                dialog.setVisible(true);
                updateDisplay();
            }
        });
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCustomerDialog dialog = new NewCustomerDialog();
                int selected_row = customerListTable.getSelectedRow();
                int model_index = customerListTable.convertRowIndexToModel(selected_row);
                int customer_id = row_to_customer_mapping.get(model_index).customer_id;
                dialog.setLocationRelativeTo(customerListTable);
                dialog.setEditStatus(true, customer_id);
                dialog.setListener(new NewCustomerDialog.CustomerListener() {
                    @Override
                    public void customerAdded() {
                        updateDisplay();
                    }

                    @Override
                    public void cancelSelected() { }
                });
                dialog.setVisible(true);
            }
        });
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PrintSingleCustomerDialog dialog = new PrintSingleCustomerDialog();
                int selected_row = customerListTable.getSelectedRow();
                int model_index = customerListTable.convertRowIndexToModel(selected_row);
                int customer_id = row_to_customer_mapping.get(model_index).customer_id;

                dialog.setPrintingValues(customer_id, current_reading_id);
                dialog.setLocationRelativeTo(customerListTable);
                dialog.setVisible(true);
                updateDisplay();
            }
        });
        printAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageFormat header = new MessageFormat("Page{0,number,integer}");
                try {
                    customerListTable.print(JTable.PrintMode.FIT_WIDTH, header, null);
                } catch (PrinterException msg) {
                    System.err.println("Error in printing " + msg.getMessage());
                }
            }
        });
        addCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewCustomerDialog dialog = new NewCustomerDialog();
                dialog.setListener(new NewCustomerDialog.CustomerListener() {
                    @Override
                    public void customerAdded() {
                        updateDisplay();
                    }

                    @Override
                    public void cancelSelected() {
                    }
                });
                dialog.setLocationRelativeTo(customerListTable);
                dialog.setVisible(true);
            }
        });
        deleteCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected_row = customerListTable.getSelectedRow();
                int model_index = customerListTable.convertRowIndexToModel(selected_row);
                int customer_id = row_to_customer_mapping.get(model_index).customer_id;
                String customer_name = row_to_customer_mapping.get(model_index).name;
                int confirm_result = JOptionPane.showConfirmDialog(null,
                        "This will delete customer: " + customer_name,
                        "Are you Sure?",
                        JOptionPane.YES_NO_OPTION);
                if (confirm_result == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try (Connection conn = DriverManager.
                                    getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                                stmt.execute("" + "delete from customer where customer_id = " + customer_id);
                            } catch (SQLException e) {
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            updateDisplay();
                        }
                    }.execute();
                }
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
                        updateDisplay();
                    }

                    @Override
                    public void cancelSelected() {
                    }
                });
                dialog.setVisible(true);
            }
        });
        deleteReadingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String current_reading_name = reading_id_names.get(selected_reading_index).getValue();
                int confirm_result = JOptionPane.showConfirmDialog(null,
                        "This will delete all data regarding " + current_reading_name,
                        "Are you Sure?",
                        JOptionPane.YES_NO_OPTION);
                if (confirm_result == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try (Connection conn = DriverManager.
                                    getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                                stmt.execute("" + "delete from reading where reading_id = " + current_reading_id);
                            } catch (SQLException e) {
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            updateDisplay();
                        }
                    }.execute();
                } else {

                }
            }
        });

        customerListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                setActionButtonStatus();
            }
        });
        updateDisplay();
    }

    void setActionButtonStatus() {
        int[] selected_rows = customerListTable.getSelectedRows();
        boolean selected = false;
        if (selected_rows.length > 0) {
            selected = true;
        }

        setReadingButton.setEnabled(selected && selected_reading_index != -1);
        printAllButton.setEnabled((customerListTable.getModel().getRowCount() != 0) &&
                (selected_reading_index != -1));
        deleteCustomerButton.setEnabled(selected);
        infoButton.setEnabled(selected);
        printButton.setEnabled(selected);
    }

    private static String[][] columnsToShow = {
            // from the customer table
            //{"Customer Name", "c.name"},
            // this will not be shown, only for indexing purposes
            {"_customer_id_", "c.customer_id"},

            {"Name", "c.name"},
            {"Floor", "c.floor"},
            {"Shop", "c.shop_location"},
            //{"Business Type", "c.type_of_business"},

            // from the reading table
            {"Reading Date", "c_r.date"},

            // from the customer_reading table
            {"Previous", "c_r.previous_reading"},
            {"Current", "c_r.current_reading"},
            //{"Below 50kW", "c_r.below_50"},
            //{"Above 50kW", "c_r.above_50"},
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
            if (index == 0 &&
                    columnsToShow[i][0].equals("_customer_id_")) continue;

            result.add(columnsToShow[i][index]);
        }
        return result;
    }

    void updateReadingValues() {
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
                selected_reading_index = -1;
                current_reading_id = -1;
            } else {
                // its our first time searching for ids
                // OR
                // there is a newly added reading, make it the current reading
                if (selected_reading_index == -1 || (previous_size != reading_id_names.size())) {
                    selected_reading_index = reading_id_names.size() - 1;
                }
                current_reading_id = reading_id_names.get(selected_reading_index).getKey();
            }
        } catch (Exception e) {
            reading_id_names = null;
            selected_reading_index = -1;

            System.err.println("Exception in Load Reading Data" + e.getMessage());
        }
    }

    void updateTableModel() {
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        row_to_customer_mapping = new HashMap<>();

        try (Connection conn = DriverManager.
                getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

            Vector<String> columnsToSelect = selectColumnIndex(1);
            String columns = CustomStringUtil.join(columnsToSelect, ",");
            String query = "" +
                    "select " + columns + " from customer c " +
                    " LEFT JOIN (select * from " +
                    "       reading r INNER JOIN customer_reading cr " +
                    "       ON r.reading_id = cr.r_id " +
                    "           WHERE r.reading_id = " + current_reading_id + " ) c_r " +
                    " ON c.customer_id = c_r.c_id ";

            ResultSet rs = stmt.executeQuery(query);

            Vector<String> columnNames = selectColumnIndex(0);

            Vector<Vector<Object>> data = new Vector<Vector<Object>>();
            while (rs.next()) {
                Vector<Object> vector = new Vector<Object>();
                CustomerInfo info = new CustomerInfo();
                for (int i = 1; i <= columnsToSelect.size(); i++) {
                    if (i == 1) {       // if it is the _customer_id_ column, don't add it to the result data
                        info.customer_id = (Integer) rs.getObject(i);
                    } else {
                        Object obj = rs.getObject(i);
                        if (obj == null) {
                            vector.add(obj);
                            continue;
                        }
                        String column = columnsToSelect.get(i - 1);
                        if (column.equals("c.name")) {
                            vector.add(obj);
                            info.name = obj.toString();
                        } else if (column.equals("c_r.total_payment")) {
                            vector.add(obj + " birr");
                        } else if (column.equals("c_r.below_50") ||
                                column.equals("c_r.above_50") ||
                                column.equals("c_r.previous_reading") ||
                                column.equals("c_r.current_reading")) {
                            vector.add(obj + " Kw");
                        } else {
                            vector.add(obj);
                        }
                    }
                }
                row_to_customer_mapping.put(data.size(), info);
                data.add(vector);
            }

            tableModel.setDataVector(data, columnNames);

        } catch (Exception e) {
            System.err.println("Exception in Load Data" + e.getMessage());
        }
    }

    class CustomerInfo {
        int customer_id;
        String name;
    }

    void updateDisplay() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                updateReadingValues();
                updateTableModel();
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

                boolean reading_exists = true;
                boolean enable_prev, enable_next;
                if (reading_id_names == null || reading_id_names.isEmpty()) {
                    enable_next = enable_prev = false;
                    reading_exists = false;
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
                deleteReadingButton.setEnabled(reading_exists);

                customerListTable.setModel(tableModel);
                setActionButtonStatus();
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

    private Map<Integer, CustomerInfo> row_to_customer_mapping = null;
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
    private JButton printButton;
    private JButton setReadingButton;
    private JButton addCustomerButton;
    private JButton deleteCustomerButton;
    private JButton newReadingButton;
    private JLabel textReadingPeriod;
    private JButton deleteReadingButton;
    private JButton printAllButton;
}
