package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.structureVizX.internal.model.RINManager;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ChimeraProcessTask extends AbstractTask {

	private StructureManager structureManager;

	@Tunable(description = "HTTP port ChimeraX is running on", required=true, gravity = 1.0)
	public int port;

	@Tunable(description = "Launch dialog", gravity = 2.0)
	public boolean launchDialog = true;

	public ChimeraProcessTask(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Initializing communications with ChimeraX");
		structureManager.initChimera(port, launchDialog);
	}

}
