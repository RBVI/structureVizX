package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ModelChangedTask extends AbstractTask {
	private StructureManager structureManager;
	static boolean modelUpdaterRunning = false;

	@Tunable(description="Data from Chimera")
	public String chimeraNotification = "";

	public ModelChangedTask(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		(new ModelUpdater()).start();
	}

  /**
   * Model updater thread
   */
  class ModelUpdater extends Thread {

    public ModelUpdater() {
    }

    public void run() {
      if (ModelChangedTask.modelUpdaterRunning) return;
      ModelChangedTask.modelUpdaterRunning = true;
      structureManager.updateModels();
      structureManager.modelChanged();
      ModelChangedTask.modelUpdaterRunning = false;
    }
  }

}
