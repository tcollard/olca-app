package org.openlca.app.editors.graphical.model;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.CommandFactory;

class LinkPart extends AbstractConnectionEditPart {

	@Override
	public void activate() {
		getModel().setEditPart(this);
		super.activate();
	}

	@Override
	protected IFigure createFigure() {
		PolylineConnection figure = new PolylineConnection();
		figure.setForegroundColor(ConnectionLink.COLOR);
		figure.setConnectionRouter(getConnectionRouter());
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setVisible(isVisible());
		getModel().figure = figure;
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.CONNECTION_ROLE, new ConnectionEditPolicy() {
			@Override
			protected Command getDeleteCommand(GroupRequest req) {
				return CommandFactory.createDeleteLinkCommand(getModel());
			}
		});
	}

	@Override
	public ConnectionLink getModel() {
		return (ConnectionLink) super.getModel();
	}

	private ProductSystemGraphEditor getEditor() {
		return GraphUtil.getEditor(getModel().provider);
	}

	private ConnectionRouter getConnectionRouter() {
		return getEditor().isRouted() ? TreeConnectionRouter.get()
				: ConnectionRouter.NULL;
	}

	private boolean isVisible() {
		if (!getModel().provider.getFigure().isVisible())
			return false;
		if (!getModel().exchange.getFigure().isVisible())
			return false;
		return true;
	}

	@Override
	public void showSourceFeedback(Request req) {
		if (req instanceof ReconnectRequest) {
			ReconnectRequest request = ((ReconnectRequest) req);
			ConnectionLink link = (ConnectionLink) request
					.getConnectionEditPart().getModel();
			ExchangeNode target = link.exchange;
			ExchangeNode source = link.provider;

			ExchangeNode n1 = request.isMovingStartAnchor() ? target : source;
			ExchangeNode n2 = request.isMovingStartAnchor() ? source : target;
			if (n1 != null) {
				ProductSystemNode productSystemNode = n1.getParent()
						.getParent().getParent();
				productSystemNode.highlightMatchingExchanges(n1);
				n1.setHighlighted(true);
			}
			if (n2 != null)
				n2.setHighlighted(true);
		}
		super.showSourceFeedback(req);
	}

	@Override
	public void eraseSourceFeedback(Request req) {
		if (req instanceof ReconnectRequest) {
			ProcessPart source = (ProcessPart) getSource();
			ProductSystemNode productSystemNode = source.getModel().getParent();
			productSystemNode.removeHighlighting();
		}
		super.eraseSourceFeedback(req);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (getFigure().isVisible()) {
			PolylineConnection figure = (PolylineConnection) getFigure();
			if (value != EditPart.SELECTED_NONE) {
				figure.setLineWidth(2);
				figure.setForegroundColor(ConnectionLink.HIGHLIGHT_COLOR);
			} else {
				figure.setLineWidth(1);
				figure.setForegroundColor(ConnectionLink.COLOR);
			}
			super.setSelected(value);
		}
	}

	@Override
	public void refreshSourceAnchor() {
		super.refreshSourceAnchor();
	}

	@Override
	public void refreshTargetAnchor() {
		super.refreshTargetAnchor();
	}

}
