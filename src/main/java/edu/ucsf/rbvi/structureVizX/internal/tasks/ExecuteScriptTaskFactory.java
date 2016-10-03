package edu.ucsf.rbvi.structureVizX.internal.tasks;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNode;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ExecuteScriptTaskFactory extends AbstractTaskFactory implements
		NodeViewTaskFactory {

	private StructureManager structureManager;

	public ExecuteScriptTaskFactory(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		return new TaskIterator(new ExecuteScriptTask(nodeView.getModel(), networkView, structureManager));
	}

}
