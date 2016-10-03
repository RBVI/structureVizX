package edu.ucsf.rbvi.structureVizX.internal.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.structureVizX.internal.model.ChimeraModel;
import edu.ucsf.rbvi.structureVizX.internal.model.ChimeraStructuralObject;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ListModelsTask extends AbstractTask implements ObservableTask {

	private StructureManager structureManager;
	private Set<ChimeraModel> chimeraModels;

	public ListModelsTask(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("List Models");
		chimeraModels = new HashSet<ChimeraModel>();

		for (ChimeraStructuralObject obj: structureManager.getAllChimObjs()) {
			if (obj instanceof ChimeraModel)
				chimeraModels.add((ChimeraModel)obj);
		}
	}

	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			String result = "";
			for (ChimeraModel model: chimeraModels) {
				result += "#"+model.getModelNumber()+" "+model.getModelName()+"\n";
			}
			return (R)result;
		}
		return (R)getResults(String.class);
	}
}
