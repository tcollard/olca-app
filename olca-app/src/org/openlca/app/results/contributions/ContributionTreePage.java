package org.openlca.app.results.contributions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.AnalyzeEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.openlca.io.xls.Excel;
import org.openlca.app.components.FileChooser;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.util.Strings;
import org.openlca.core.model.descriptors.FlowDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TDoubleArrayList;

public class ContributionTreePage extends FormPage {

	private final FullResult result;
	private final CalculationSetup setup;
	private final ResultItemView resultItems;

	private TreeViewer tree;
	private Object selection;

	private File file;
	private Sheet sheet;
	private int row;
	private int maxColumn;
	private double totalResult;
	private final TDoubleArrayList resultValues = new TDoubleArrayList(100);
	// private final TDoubleArrayList requiredAmountValues = new TDoubleArrayList(100);
	private final List<String> requiredAmountValues = new ArrayList<String>();
	private UpstreamTree upStreamTree;
	private int maxDepth = 2;






	public ContributionTreePage(AnalyzeEditor editor) {
		super(editor, "analysis.ContributionTreePage", M.ContributionTree);
		this.result = editor.result;
		this.setup = editor.setup;
		this.resultItems = editor.resultItems;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {

		FormToolkit tk = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
			Labels.name(setup.target()),
			Images.get(result));
		Composite body = UI.formBody(form, tk);
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 2);
		var selector = ResultItemSelector
			.on(resultItems)
			.withSelectionHandler(new SelectionHandler())
			.create(comp, tk);

		Composite exportComp = tk.createComposite(body);
		createExportButton(exportComp, tk);
		UI.gridLayout(exportComp, 1);

		Composite treeComp = tk.createComposite(body);

		UI.gridLayout(treeComp, 1);
		UI.gridData(treeComp, true, true);
		createTree(tk, treeComp);
		form.reflow(true);
		selector.initWithEvent();
	}


	private static class Path {
		final Path prefix;
		final UpstreamNode node;
		final int length;

		Path(UpstreamNode node) {
			this.prefix = null;
			this.node = node;
			this.length = 0;
		}

		Path(UpstreamNode node, Path prefix) {
			this.prefix = prefix;
			this.node = node;
			this.length = 1 + prefix.length;
		}

		Path append(UpstreamNode node) {
			return new Path(node, this);
		}

		int count (TechFlow techFlow) {
			int c = Objects.equals(techFlow, node.provider()) ? 1 : 0;
			return prefix != null ? c + prefix.count(techFlow) : c;
		}
	}


	private void createExportButton(Composite comp, FormToolkit tk) {
		Logger log = LoggerFactory.getLogger(getClass());

		var b = tk.createButton(comp, "Impacts: " + M.ExportToExcel, SWT.NONE);
		b.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(b, $ -> {
			var allImpacts = this.resultItems.impacts();

			file = FileChooser.forSavingFile(
					M.Export, "contribution_tree.xlsx");

			// CREATION OF EXCEL DOCUMENT
			try (var wb = new XSSFWorkbook()){
	
				allImpacts.forEach((elem) -> {
					sheet = wb.createSheet(elem.name);

					var header = Excel.headerStyle(wb);
					Excel.cell(sheet, 0, 0, "Impact category: " + elem.name).ifPresent(c -> c.setCellStyle(header));
					Excel.cell(sheet, 1, 0, "Processes").ifPresent(c -> c.setCellStyle(header));


					upStreamTree = result.getTree(elem);
					tree.setInput(upStreamTree);

					// write the tree
					row = 1;
					maxColumn = 0;
					totalResult = upStreamTree.root.result();
					Path path = new Path(upStreamTree.root);
					traverse(path);

					// write the all values
					var unit = unit();
					var resultHeader = Strings.nullOrEmpty(unit) ? "Result" : "Result [" + unit + "]";
					var contributionHeader = "Contribution";
					var requiredAmountHeader = "Required Amount";
					Excel.cell(sheet, 1, maxColumn + 1, requiredAmountHeader)
						.ifPresent(c -> c.setCellStyle(header));
					Excel.cell(sheet, 1, maxColumn + 2, resultHeader)
						.ifPresent(c -> c.setCellStyle(header));
					Excel.cell(sheet, 1, maxColumn + 3, contributionHeader)
						.ifPresent(c -> c.setCellStyle(header));
					for (int i = 0; i < resultValues.size(); i++) {
						Excel.cell(sheet, i + 2, maxColumn + 1, requiredAmountValues.get(i));
						Excel.cell(sheet, i + 2, maxColumn + 2, resultValues.get(i));
						var percent = resultValues.get(i) / resultValues.get(0) * 100;
						Excel.cell(sheet, i + 2, maxColumn + 3, percent + " %");
					}
					resultValues.clear();
					requiredAmountValues.clear();
					
				});
				// write the file
				try(var fout = new FileOutputStream(file);
					var buff = new BufferedOutputStream(fout)){
					wb.write(buff);
				} catch (Exception e) {
					log.error("Error buffer file", e);
				}


			} catch (Exception e) {
				log.error("Contribution tree export failed", e);
				throw new RuntimeException(e);
			}
		});
	}

	private String unit() {
		var ref = upStreamTree.ref;
		if (ref == null)
			return "";
		if (ref instanceof EnviFlow)
			return Labels.refUnit((EnviFlow) ref);
		if (ref instanceof FlowDescriptor)
			return Labels.refUnit((FlowDescriptor) ref);
		if (ref instanceof ImpactDescriptor)
			return ((ImpactDescriptor) ref).referenceUnit;
		if (ref instanceof CostResultDescriptor)
			return Labels.getReferenceCurrencyCode();
		return "";
	}

	private void write(Path path) {
		row++;
		resultValues.add(path.node.result());
		requiredAmountValues.add(path.node.requiredAmount() + " " + Labels.refUnit(path.node.provider()));
		int col = path.length;
		maxColumn = Math.max(col, maxColumn);
		var node = path.node;
		if (node.provider() == null || node.provider().provider() == null)
			return;
		var label = Labels.name(node.provider().provider());
		Excel.cell(sheet, row, col, label);
	}

	private void traverse(Path path) {
		if (row >= 1048574) {
			return;
		}

		var node = path.node;
		double result = path.node.result();

		if (result == 0)
			return;
		if (maxDepth > 0 && path.length > maxDepth)
			return;

		write(path);
		for (var child : upStreamTree.childs(node)) {
			traverse(path.append(child));
		}
	}

	private void createTree(FormToolkit tk, Composite comp) {

		var headers = new String[]{
			M.Contribution,
			M.Process,
			"Required amount",
			M.Result};
		tree = Trees.createViewer(comp, headers,
			new ContributionLabelProvider());

		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.setContentProvider(new ContributionContentProvider());
		tk.adapt(tree.getTree(), false, false);
		tk.paintBordersFor(tree.getTree());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		tree.getTree().getColumns()[3].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(tree.getTree(),
			0.20, 0.40, 0.20, 0.20);

		// action bindings
		Action onOpen = Actions.onOpen(() -> {

			UpstreamNode n = Viewers.getFirstSelected(tree);
			if (n == null || n.provider() == null)
				return;
			App.open(n.provider().provider());
		});

		Action onExport = Actions.create(M.ExportToExcel,
			Images.descriptor(FileType.EXCEL), () -> {
				Object input = tree.getInput();
				if (!(input instanceof UpstreamTree))
					return;
				TreeExportDialog.open((UpstreamTree) input);
			});

		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree), onExport);
		Trees.onDoubleClick(tree, e -> onOpen.run());
	}

	private class SelectionHandler implements ResultItemSelector.SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			selection = flow;
			UpstreamTree model = result.getTree(flow);
			tree.setInput(model);
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {

			selection = impact;
			UpstreamTree model = result.getTree(impact);

			tree.setInput(model);
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			selection = cost;
			UpstreamTree model = cost.forAddedValue
				? result.getAddedValueTree()
				: result.getCostTree();
			tree.setInput(model);
		}
	}

	private static class ContributionContentProvider implements ITreeContentProvider {

		private UpstreamTree tree;

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof UpstreamNode node))
				return null;
			if (tree == null)
				return null;
			return tree.childs(node).toArray();
		}

		@Override
		public Object[] getElements(Object input) {
			return input instanceof UpstreamTree tree
				? new Object[]{tree.root}
				: null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof UpstreamNode node))
				return false;
			return !tree.childs(node).isEmpty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object input) {
			if (!(input instanceof UpstreamTree)) {
				tree = null;
				return;
			}
			tree = (UpstreamTree) input;
		}

		@Override
		public void dispose() {
		}

	}

	private class ContributionLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			if (col == 1 && node.provider() != null)
				return Images.get(node.provider().provider());
			if (col == 3)
				return image.get(getContribution(node));
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UpstreamNode node))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(getContribution(node));
				case 1 -> Labels.name(node.provider().provider());
				case 2 -> Numbers.format(node.requiredAmount()) + " "
					+ Labels.refUnit(node.provider());
				case 3 -> Numbers.format(node.result()) + " " + getUnit();
				default -> null;
			};
		}

		private String getUnit() {
			if (selection instanceof EnviFlow flow) {
				return Labels.refUnit(flow);
			} else if (selection instanceof ImpactDescriptor impact) {
				return impact.referenceUnit;
			} else if (selection instanceof CostResultDescriptor) {
				return Labels.getReferenceCurrencyCode();
			}
			return null;
		}

		private double getContribution(UpstreamNode node) {
			if (node.result() == 0)
				return 0;
			double total = ((UpstreamTree) tree.getInput()).root.result();
			if (total == 0)
				return 0;
			return total < 0 && node.result() > 0
				? -node.result() / total
				: node.result() / total;
		}
	}
}
