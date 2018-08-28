package org.openlca.app.wizards;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderDialog extends Dialog {

	public static long select(CalcExchange e, long[] providers) {
		if (providers == null || providers.length == 0)
			return 0L;
		if (e == null)
			return providers[0];
		AtomicLong val = new AtomicLong(providers[0]);
		try {
			App.runInUI("#Select a provider", () -> {
				ProviderDialog dialog = new ProviderDialog(e, providers);
				dialog.open();
				val.set(dialog.selected);
			}).join();
		} catch (Exception ex) {
			Logger log = LoggerFactory.getLogger(ProviderDialog.class);
			log.error("Failed to wait for dialog thread", ex);
		}
		return val.get();
	}

	private long selected;
	private final CalcExchange exchange;
	private final long[] providers;

	public ProviderDialog(CalcExchange e, long[] providers) {
		super(UI.shell());
		this.exchange = e;
		this.providers = providers;
		this.selected = providers[0];
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		UI.gridLayout(container, 2);
		UI.formLabel(container, M.Process);
		UI.formLabel(container, getLabel(
				ProcessDescriptor.class, exchange.processId));
		UI.formLabel(container, M.Flow);
		UI.formLabel(container, getLabel(
				FlowDescriptor.class, exchange.flowId));
		Combo combo = UI.formCombo(container, M.Providers);
		String[] labels = new String[providers.length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = getLabel(ProcessDescriptor.class, providers[i]);
		}
		combo.setItems(labels);
		combo.select(0);
		Controls.onSelect(combo, e -> {
			selected = providers[combo.getSelectionIndex()];
		});
		UI.formCheckBox(container, "").setText(
				"#Always use this provider for this flow");
		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("#Select a provider");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	private <T extends BaseDescriptor> String getLabel(Class<T> clazz, long id) {
		EntityCache cache = Cache.getEntityCache();
		if (cache == null)
			return "?";
		T d = cache.get(clazz, id);
		if (d == null)
			return "?";
		return Labels.getDisplayName(d);
	}

}
