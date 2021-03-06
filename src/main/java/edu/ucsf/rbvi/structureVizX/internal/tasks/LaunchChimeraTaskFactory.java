package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class LaunchChimeraTaskFactory extends AbstractTaskFactory implements TaskFactory {

	private StructureManager structureManager;
	
	public LaunchChimeraTaskFactory(StructureManager structureManager) {
		this.structureManager = structureManager;
	}
	
	public boolean isReady() {
		if (!structureManager.getChimeraManager().isChimeraLaunched()) {
			return true;
		}
		return false;
	}

	
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new LaunchChimeraTask(structureManager));
	}

}
