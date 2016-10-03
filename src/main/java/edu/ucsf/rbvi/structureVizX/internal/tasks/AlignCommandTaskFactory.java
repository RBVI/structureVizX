package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class AlignCommandTaskFactory implements TaskFactory {

	private StructureManager structureManager;

	public AlignCommandTaskFactory(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AlignCommandTask(structureManager));
	}

	public boolean isReady() {
		return true;
	}

}
