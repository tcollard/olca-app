package org.openlca.app.editors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.EventHandler;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.Version;

/**
 * This is the general info section that each editor has: name, description,
 * etc.
 */
public class InfoSection {

	private CategorizedEntity entity;
	private final DataBinding binding;
	private final ModelEditor<?> editor;

	private Composite container;
	private Label versionLabel;

	public InfoSection(ModelEditor<?> editor) {
		this.entity = editor.getModel();
		this.editor = editor;
		this.binding = new DataBinding(editor);
	}

	public void render(Composite body, FormToolkit toolkit) {
		container = UI.formSection(body, toolkit, M.GeneralInformation);
		Text nameText = UI.formText(container, toolkit, M.Name);
		binding.onString(() -> editor.getModel(), "name", nameText);
		Text descriptionText = UI.formMultiText(container, toolkit,
				M.Description);
		binding.onString(() -> editor.getModel(), "description",
				descriptionText);
		if (entity.getCategory() != null) {
			new Label(container, SWT.NONE).setText(M.Category);
			createBreadcrumb(container);
		}
		createVersionText(toolkit);
		Text uuidText = UI.formText(container, toolkit, "UUID");
		uuidText.setEditable(false);
		if (entity.getRefId() != null)
			uuidText.setText(entity.getRefId());
		createDateText(toolkit);
	}

	private void createVersionText(FormToolkit toolkit) {
		UI.formLabel(container, toolkit, M.Version);
		Composite composite = toolkit.createComposite(container);
		GridLayout layout = UI.gridLayout(composite, 3);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		versionLabel = toolkit.createLabel(composite,
				Version.asString(entity.getVersion()));
		editor.onSaved(new EventHandler() {
			@Override
			public void handleEvent() {
				entity = editor.getModel();
				versionLabel.setText(Version.asString(entity.getVersion()));
			}
		});
		new VersionLink(composite, toolkit, VersionLink.MAJOR);
		new VersionLink(composite, toolkit, VersionLink.MINOR);
	}

	private void createDateText(FormToolkit toolkit) {
		final SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ssZ");
		UI.formLabel(container, toolkit, M.LastChange);
		final Label text = UI.formLabel(container, toolkit, "");
		if (entity.getLastChange() != 0)
			text.setText(format.format(new Date(entity.getLastChange())));
		editor.onSaved(new EventHandler() {
			@Override
			public void handleEvent() {
				entity = editor.getModel();
				text.setText(format.format(new Date(entity.getLastChange())));
			}
		});
	}

	private void createBreadcrumb(Composite parent) {
		Stack<Category> stack = new Stack<>();
		Category current = entity.getCategory();
		while (current != null) {
			stack.push(current);
			current = current.getCategory();
		}
		Composite breadcrumb = new Composite(parent, SWT.NONE);
		UI.gridLayout(breadcrumb, stack.size() * 2 - 1, 0, 0);
		while (!stack.isEmpty()) {
			current = stack.pop();
			Hyperlink link = null;
			if (current.getCategory() == null) {
				link = new ImageHyperlink(breadcrumb, SWT.NONE);
				((ImageHyperlink) link).setImage(Images.get(current));
			} else {
				new Label(breadcrumb, SWT.NONE).setText(" > ");
				link = new Hyperlink(breadcrumb, SWT.NONE);
			}
			link.setText(current.getName());
			link.addHyperlinkListener(new CategoryLinkClick(current));
			link.setForeground(Colors.linkBlue());
		}
	}

	public Composite getContainer() {
		return container;
	}

	private class CategoryLinkClick extends HyperlinkAdapter {

		private Category category;

		private CategoryLinkClick(Category category) {
			this.category = category;
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			Navigator.select(category);
		}
	}

	private class VersionLink extends HyperlinkAdapter {

		static final int MAJOR = 1;
		static final int MINOR = 2;
		private final int type;
		private final ImageHyperlink link;

		private Image hoverIcon = null;
		private Image icon = null;
		private String tooltip = null;

		public VersionLink(Composite parent, FormToolkit toolkit, int type) {
			this.type = type;
			link = toolkit.createImageHyperlink(parent, SWT.TOP);
			link.addHyperlinkListener(this);
			configureLink();
		}

		private void configureLink() {
			if (type == MAJOR) {
				tooltip = M.UpdateMajorVersion;
				hoverIcon = Icon.UP.get();
				icon = Icon.UP_DISABLED.get();
			} else {
				tooltip = M.UpdateMinorVersion;
				hoverIcon = Icon.UP_DOUBLE.get();
				icon = Icon.UP_DOUBLE_DISABLED.get();
			}
			link.setToolTipText(tooltip);
			link.setActiveImage(hoverIcon);
			link.setImage(icon);
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			if (entity == null || versionLabel == null)
				return;
			Version version = new Version(entity.getVersion());
			if (type == MAJOR)
				version.incMajor();
			else
				version.incMinor();
			entity.setVersion(version.getValue());
			versionLabel.setText(version.toString());
			editor.setDirty(true);
		}

		@Override
		public void linkEntered(HyperlinkEvent e) {
			link.setImage(hoverIcon);
		}

		@Override
		public void linkExited(HyperlinkEvent e) {
			link.setImage(icon);
		}
	}
}
