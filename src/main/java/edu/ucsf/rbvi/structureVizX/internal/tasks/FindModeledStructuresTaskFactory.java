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

public class FindModeledStructuresTaskFactory extends AbstractTaskFactory implements
		NodeViewTaskFactory {

	private StructureManager structureManager;

	public FindModeledStructuresTaskFactory(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		List<CyNode> nodeList = new ArrayList<CyNode>();
		nodeList.add(nodeView.getModel());
		return new TaskIterator(new FindModeledStructuresTask(nodeList, networkView, structureManager));
	}

}
