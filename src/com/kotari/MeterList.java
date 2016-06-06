package com.kotari;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MeterList extends JDialog {
    private DialogOperationListener mListener;
    public void setListener(DialogOperationListener listener) { mListener = listener; }

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable dialog_meter_list_table;
    private JButton dialog_meter_list_btn_add;
    private JButton dialog_meter_list_btn_edit;
    private JButton dialog_meter_list_btn_history;
    private JButton dialog_meter_list_btn_delete;
    private JButton dialog_meter_list_close;

    private Meter getSelectedMeterInfo() {
        int selected_row = dialog_meter_list_table.getSelectedRow();
        int model_index = dialog_meter_list_table.convertRowIndexToModel(selected_row);
        return row_to_meter_mapping.get(model_index);
    }

    public MeterList() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setMinimumSize(new Dimension(550, 300));

        setTitle("Electric Meters");

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
        dialog_meter_list_close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        dialog_meter_list_btn_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewMeterDialog dialog = new NewMeterDialog();
                dialog.setListener(new DialogOperationListener() {
                    @Override
                    public void operationFinished() {
                        updateDisplay();
                    }

                    @Override
                    public void operationCanceled() {

                    }
                });
                dialog.setLocationRelativeTo(dialog_meter_list_table);
                dialog.setVisible(true);
                updateDisplay();
            }
        });
        dialog_meter_list_btn_edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int meter_id = getSelectedMeterInfo().meter_id;
                NewMeterDialog dialog = new NewMeterDialog();
                dialog.setMeterDialogArguments(true, meter_id);
                dialog.setListener(new DialogOperationListener() {
                    @Override
                    public void operationFinished() {
                        updateDisplay();
                    }

                    @Override
                    public void operationCanceled() {

                    }
                });
                dialog.setLocationRelativeTo(dialog_meter_list_table);
                dialog.setVisible(true);
                updateDisplay();
            }
        });
        dialog_meter_list_btn_history.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        dialog_meter_list_btn_delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int meter_id = getSelectedMeterInfo().meter_id;
                String property_number = getSelectedMeterInfo().meter_property_number;

                int confirm_result = JOptionPane.showConfirmDialog(null,
                        "This will delete the meter: " + property_number,
                        "Are you Sure? ",
                        JOptionPane.YES_NO_OPTION);
                if (confirm_result == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try (Connection conn = DriverManager.
                                    getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {
                                stmt.execute("delete from meter where meter_id = " + meter_id);
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
        dialog_meter_list_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dialog_meter_list_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                setActionButtonStatus();
            }
        });
        updateDisplay();
    }

    void onCancel() {
        mListener.operationCanceled();
        dispose();
    }

    void setActionButtonStatus() {
        int[] selected_rows = dialog_meter_list_table.getSelectedRows();
        boolean is_selected = false;
        if (selected_rows.length > 0) {
            is_selected = true;
        }

        dialog_meter_list_btn_edit.setEnabled(is_selected);
        dialog_meter_list_btn_history.setEnabled(is_selected);
        dialog_meter_list_btn_delete.setEnabled(is_selected);
    }

    void updateDisplay() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                updateTableModel();
                return null;
            }

            @Override
            protected void done() {
                dialog_meter_list_table.setModel(tableModel);
                setActionButtonStatus();
            }
        }.execute();
    }

    private static String [][]columnsTable = {
            /**
             * Any fields in the first column that start with "_"
             * will not be shown in the table.
            */
            {"_meter_id_", "meter_id"},

            {"Property Number", "property_number"},
            {"Initial Reading", "initial_reading"},
            {"Date of Install", "date_of_install"},
            {"Floor", "floor"},
            {"Shop", "shop"},

            {"Current Customer", "name"}
    };

    private Vector<String> selectColumnAtIndex(int index) {
        if (index > 1) index = 1;
        Vector<String> result = new Vector<>();
        for (int i = 0; i < columnsTable.length; i++) {
            if (index == 0 &&
                columnsTable[i][0].startsWith("_")) continue;

            result.add(columnsTable[i][index]);
        }
        return result;
    }

    void updateTableModel() {
        // clear out previous data
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        row_to_meter_mapping = new HashMap<>();

        try (Connection conn = DriverManager.
                getConnection(DbUtil.connection_string); Statement stmt = conn.createStatement()) {

            Vector<String> columnsToSelect = selectColumnAtIndex(1);
            String _columns = CustomStringUtil.join(columnsToSelect, ", ");
            String query = "" +
                    "select " + _columns + " from meter LEFT JOIN customer on " +
                    " customer.customer_meter_id = meter.meter_id " +
                    " where meter_id != " + DbUtil.DEFAULT_METER_ID;

            ResultSet rs = stmt.executeQuery(query);

            Vector<String> columnNames = selectColumnAtIndex(0);
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                Meter info = new Meter();
                for (int i = 1; i <= columnsToSelect.size(); i++) {
                    if (i == 1) {   // the _meter_id column
                        info.meter_id = (Integer) rs.getObject(1);
                    } else {
                        Object obj = rs.getObject(i);
                        if (obj == null) {
                            row.add(obj);
                            continue;
                        }
                        String column = columnsToSelect.get(i - 1);
                        if (column.equals("name")) {
                            row.add(obj);
                            info.current_customer_name = obj.toString();
                        } else if (column.equals("property_number")) {
                            row.add(obj);
                            info.meter_property_number = obj.toString();
                        } else {
                            row.add(obj);
                        }
                    }
                }
                row_to_meter_mapping.put(data.size(), info);
                data.add(row);
            }

            tableModel.setDataVector(data, columnNames);
        } catch (SQLException e) {
            System.err.println("Exception in load meter data: " + e.getMessage());
        }
    }

    private DefaultTableModel tableModel;
    private Map<Integer, Meter> row_to_meter_mapping = null;

    public static void main(String[] args) {
        MeterList dialog = new MeterList();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
