package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.util.Colors;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ColorfulTheme implements Theme {

	static final Color COLOR_LIGHT_GREY = Colors.get(242, 242, 242);
	static final Color COLOR_WHITE = Colors.white();

	private static final Color COLOR_DARK_GREY = Colors.get(64, 64, 64);

	private static final Color COLOR_PRODUCT = Colors.get(0, 0, 102);
	private static final Color COLOR_WASTE = Colors.get(158, 72, 14);
	private static final Color COLOR_LIBRARY = Colors.get(0, 176, 240);
	private static final Color COLOR_SYSTEM = Colors.get(0, 111, 54);
	private static final Color DEFAULT_BORDER = Colors.get(128, 0, 128);

	@Override
	public String label() {
		return "Colorful";
	}

	@Override
	public String id() {
		return "colorful";
	}
	
	@Override
	public Color graphBorderColor() {
		return DEFAULT_BORDER;
	}

	@Override
	public Color graphBackground() {
		return COLOR_LIGHT_GREY;
	}

	@Override
	public Color colorOf(Link link) {
		if (link == null)
			return COLOR_DARK_GREY;
		var provider = link.provider();
		if (provider == null)
			return COLOR_DARK_GREY;
		return COLOR_DARK_GREY;
	}

	@Override
	public Color defaultLinkColor() {
		return COLOR_PRODUCT;
	}

	@Override
	public Color graphForeground() {
		return COLOR_DARK_GREY;
	}

	@Override
	public Color ioHeaderForegroundOf(ProcessNode node) {
		return COLOR_WHITE;
	}

	@Override
	public Color ioForegroundOf(ExchangeNode node) {
		if (node == null)
			return COLOR_DARK_GREY;
		var flowType = node.flowType();
		if (flowType == null)
			return COLOR_DARK_GREY;
		switch (flowType) {
			case PRODUCT_FLOW:
				return COLOR_PRODUCT;
			case WASTE_FLOW:
				return COLOR_WASTE;
			default:
				return COLOR_DARK_GREY;
		}
	}

	private static boolean isUnitProcess(ProcessNode node) {
		if (node == null)
			return false;
		var d = node.process;
		if (d.isFromLibrary())
			return false;
		if (!(d instanceof ProcessDescriptor))
			return false;
		var p = (ProcessDescriptor) d;
		return p.processType == ProcessType.UNIT_PROCESS;
	}
}
