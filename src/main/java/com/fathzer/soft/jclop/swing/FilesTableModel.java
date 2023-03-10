package com.fathzer.soft.jclop.swing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.fathzer.soft.jclop.Entry;


@SuppressWarnings("serial")
class FilesTableModel extends AbstractTableModel {
	private List<Entry> rows;

	public FilesTableModel() {
		super();
		rows = new ArrayList<Entry>();
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rows.get(rowIndex).getDisplayName();
	}

	void clear() {
		rows.clear();
		fireTableDataChanged();
	}
	
	void add(Entry rentry) {
		int len = getRowCount();
		rows.add(rentry);
		fireTableRowsInserted(len, len);
	}
	
	protected Entry getEntry(int row) {
		return rows.get(row);
	}

	protected int indexOf(Entry entry) {
		return rows.indexOf(entry);
	}
}
