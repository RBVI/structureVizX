package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ChimeraProcessTaskFactory extends AbstractTaskFactory {

	private StructureManager structureManager;

	public ChimeraProcessTaskFactory(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ChimeraProcessTask(structureManager));
	}
}
