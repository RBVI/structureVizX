package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ShowDialogTask extends AbstractTask {

	private StructureManager structureManager;
	
	public ShowDialogTask(StructureManager structureManager) {
		this.structureManager = structureManager; 
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		// open dialog
		taskMonitor.setTitle("Opening Cytoscape Molecular Structure Navigator");
		structureManager.launchModelNavigatorDialog();
	}

}
