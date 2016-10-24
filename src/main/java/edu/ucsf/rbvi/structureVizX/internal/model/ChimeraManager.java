package edu.ucsf.rbvi.structureVizX.internal.model;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.structureVizX.internal.io.ChimeraIO;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager.ModelType;

/**
 * This object maintains the Chimera communication information.
 */
public class ChimeraManager {
	static private Map<String, ChimeraModel> currentModelsMap;

	private StructureManager structureManager;
	private ChimeraIO chimera;

	public ChimeraManager(StructureManager structureManager, ChimeraIO chimera) {
		this.structureManager = structureManager;
		currentModelsMap = new HashMap<String, ChimeraModel>();
		this.chimera = chimera;
	}

	public List<ChimeraModel> getChimeraModels(String modelName) {
		List<ChimeraModel> models = getChimeraModels(modelName, ModelType.PDB_MODEL);
		models.addAll(getChimeraModels(modelName, ModelType.SMILES));
		return models;
	}

	public List<ChimeraModel> getChimeraModels(String modelName, ModelType modelType) {
		List<ChimeraModel> models = new ArrayList<ChimeraModel>();
		for (ChimeraModel model : currentModelsMap.values()) {
			if (modelName.equals(model.getModelName()) && modelType.equals(model.getModelType())) {
				models.add(model);
			}
		}

		// If we didn't find the model, see if it's an atom spec
		if (modelName.indexOf("#") >= 0) {
			AtomSpec spec = AtomSpec.getChimeraXAtomSpec(modelName, structureManager);
			if (spec != null) {
				ChimeraModel model = getChimeraModel(spec);
				models.add(model);
			}
		}
		return models;
	}

	public Map<String, List<ChimeraModel>> getChimeraModelsMap() {
		Map<String, List<ChimeraModel>> models = new HashMap<String, List<ChimeraModel>>();
		for (ChimeraModel model : currentModelsMap.values()) {
			String modelName = model.getModelName();
			if (!models.containsKey(modelName)) {
				models.put(modelName, new ArrayList<ChimeraModel>());
			}
			if (!models.get(modelName).contains(model)) {
				models.get(modelName).add(model);
			}
		}
		return models;
	}

	public ChimeraModel getChimeraModel(Integer modelNumber, String[] subModelIds) {
		String key = ChimUtils.makeModelKey(modelNumber, subModelIds);
		if (currentModelsMap.containsKey(key)) {
			return currentModelsMap.get(key);
		}
		return null;
	}

	public ChimeraModel getChimeraModel(AtomSpec spec) {
		String key = ChimUtils.makeModelKey(spec.getModelNumber(), spec.getSubModelIds());
		if (currentModelsMap.containsKey(key)) {
			return currentModelsMap.get(key);
		}
		return null;
	}

	public ChimeraModel getChimeraModel() {
		return currentModelsMap.values().iterator().next();
	}

	public Collection<ChimeraModel> getChimeraModels() {
		// this method is invoked by the model navigator dialog
		return currentModelsMap.values();
	}

	public int getChimeraModelsCount(boolean smiles) {
		// this method is invokes by the model navigator dialog
		int counter = currentModelsMap.size();
		if (smiles) {
			return counter;
		}

		for (ChimeraModel model : currentModelsMap.values()) {
			if (model.getModelType() == ModelType.SMILES) {
				counter--;
			}
		}
		return counter;
	}

	public boolean hasChimeraModel(Integer modelNubmer) {
		return hasChimeraModel(modelNubmer, null);
	}

	public boolean hasChimeraModel(Integer modelNubmer, String[] subModels) {
		return currentModelsMap.containsKey(ChimUtils.makeModelKey(modelNubmer, subModels));
	}

	public void addChimeraModel(Integer modelNumber, String[] subModels, ChimeraModel model) {
		currentModelsMap.put(ChimUtils.makeModelKey(modelNumber, subModels), model);
	}

	public void removeChimeraModel(Integer modelNumber, String[] subModels) {
		String modelKey = ChimUtils.makeModelKey(modelNumber, subModels);
		if (currentModelsMap.containsKey(modelKey)) {
			currentModelsMap.remove(modelKey);
		}
	}

	public List<ChimeraModel> openModel(String modelPath, ModelType type) {
		structureManager.logInfo("chimera open " + modelPath);
		chimera.stopListening();
		List<String> response = null;

		// Get the current list of open models
		List<ChimeraModel> modelList = getModelList();

		// TODO: [Optional] Handle modbase models
		if (type == ModelType.MODBASE_MODEL) {
			response = chimera.sendChimeraCommand("open modbase:" + modelPath, true);
			// } else if (type == ModelType.SMILES) {
			// response = chimera.sendChimeraCommand("open smiles:" + modelName, true);
			// modelName = "smiles:" + modelName;
		} else {
			response = chimera.sendChimeraCommand("open " + modelPath, true);
		}
		if (response == null) {
			// something went wrong
			structureManager.logWarning("Could not open " + modelPath);
			return null;
		}

		List<ChimeraModel> models = new ArrayList<ChimeraModel>();

		for (ChimeraModel model: getModelList()) {
			for (ChimeraModel oldModel: modelList) {
				if (model.getModelNumber() != oldModel.getModelNumber())
					break;
			}
			models.add(model);
			addChimeraModel(model.getModelNumber(), model.getSubModelIds(), model);
		}

		// assign color and residues to open models
		for (ChimeraModel newModel : models) {
			// get model color
			Color modelColor = getModelColor(newModel);
			if (modelColor != null) {
				newModel.setModelColor(modelColor);
			}

			// Get our properties (default color scheme, etc.)
			// Make the molecule look decent
			// chimeraSend("repr stick "+newModel.toSpec());

			// Create the information we need for the navigator
			if (type != ModelType.SMILES) {
				addResidues(newModel);
			}
		}

		chimera.sendChimeraCommand("view", false);
		chimera.startListening();
		return models;
	}

	public void closeModel(ChimeraModel model) {
		if (model == null) return;

		chimera.stopListening();
		structureManager.logInfo("chimera close model " + model.getModelName());
		if (currentModelsMap.containsKey(ChimUtils.makeModelKey(model.getModelNumber(),
				model.getSubModelIds()))) {
			chimera.sendChimeraCommand("close " + model.toSpec(), false);
			// currentModelNamesMap.remove(model.getModelName());
			currentModelsMap.remove(ChimUtils.makeModelKey(model.getModelNumber(),
					model.getSubModelIds()));
			// selectionList.remove(chimeraModel);
		} else {
			structureManager.logWarning("Could not find model " + model.getModelName() + " to close.");
		}
		chimera.startListening();
	}

	public void focus() {
		chimera.sendChimeraCommand("focus", false);
	}

	public Map<String, ChimeraModel> getSelectedModels() {
		Map<String, ChimeraModel> selectedModelsMap = new HashMap<String, ChimeraModel>();
		List<String> chimeraReply = chimera.sendChimeraCommand("listinfo selection level molecule", true);
		if (chimeraReply != null) {
			for (String modelLine : chimeraReply) {
				AtomSpec spec = AtomSpec.getListInfoAtomSpec(modelLine, structureManager);
				ChimeraModel chimeraModel = new ChimeraModel(spec);
				String modelKey = ChimUtils.makeModelKey(chimeraModel.getModelNumber(),
						chimeraModel.getSubModelIds());
				selectedModelsMap.put(modelKey, chimeraModel);
			}
		}
		return selectedModelsMap;
	}

	public List<String> getSelectedResidueSpecs() {
		List<String> selectedResidues = new ArrayList<String>();
		List<String> chimeraReply = chimera.sendChimeraCommand("listinfo selection level residue", true);
		if (chimeraReply != null) {
			for (String inputLine : chimeraReply) {
				AtomSpec spec = AtomSpec.getListInfoAtomSpec(inputLine, structureManager);
				selectedResidues.add(spec.toSpec());
				// String[] inputLineParts = inputLine.split("\\s+");
				// if (inputLineParts.length == 5) {
				// 	selectedResidues.add(inputLineParts[2]);
				// }
			}
		}
		return selectedResidues;
	}

	public void getSelectedResidues(Map<String, ChimeraModel> selectedModelsMap) {
		List<String> chimeraReply = chimera.sendChimeraCommand("listinfo selection level residue", true);
		if (chimeraReply != null) {
			for (String inputLine : chimeraReply) {
				AtomSpec spec = AtomSpec.getListInfoAtomSpec(inputLine, structureManager);
				ChimeraResidue r = new ChimeraResidue(spec);
				String modelKey = ChimUtils
						.makeModelKey(r.getModelNumber(), r.getSubModelIds());
				if (selectedModelsMap.containsKey(modelKey)) {
					ChimeraModel model = selectedModelsMap.get(modelKey);
					model.addResidue(r);
				}
			}
		}
	}

	/**
	 * Return the list of ChimeraModels currently open. Warning: if smiles model name too long, only
	 * part of it with "..." is printed.
	 * 
	 * 
	 * @return List of ChimeraModel's
	 */
	// TODO: [Optional] Handle smiles names in a better way in Chimera?
	public List<ChimeraModel> getModelList() {
		List<ChimeraModel> modelList = new ArrayList<ChimeraModel>();
		List<String> list = chimera.sendChimeraCommand("listinfo models type AtomicStructure", true);
		if (list != null && list.size() > 0) {
			for (String modelLine : list) {
				// System.out.println("getModelList: line: '"+modelLine+"'");
				AtomSpec spec = AtomSpec.getListInfoAtomSpec(modelLine, structureManager);
				ChimeraModel chimeraModel = new ChimeraModel(spec);
				modelList.add(chimeraModel);
			}
		}
		return modelList;
	}

	/**
	 * Return the list of depiction presets available from within Chimera. Chimera will return the
	 * list as a series of lines with the format: Preset type number "description"
	 * 
	 * @return list of presets
	 */
	public List<String> getPresets() {
		ArrayList<String> presetList = new ArrayList<String>();
		/* ChimeraX doesn't have presets (yet)
		List<String> output = chimera.sendChimeraCommand("preset list", true);
		if (output != null) {
			for (String preset : output) {
				preset = preset.substring(7); // Skip over the "Preset"
				preset = preset.replaceFirst("\"", "(");
				preset = preset.replaceFirst("\"", ")");
				// string now looks like: type number (description)
				presetList.add(preset);
			}
		}
		*/
		return presetList;
	}

	/**
	 * Determine the color that Chimera is using for this model.
	 * 
	 * @param model
	 *            the ChimeraModel we want to get the Color for
	 * @return the default model Color for this model in Chimera
	 */
	public Color getModelColor(ChimeraModel model) {
		List<String> colorLines = chimera.sendChimeraCommand("listinfo models " + model.toSpec()
				+ " attribute color type AtomicStructure", true);
		if (colorLines == null || colorLines.size() == 0) {
			return null;
		}
		return ChimUtils.parseModelColor((String) colorLines.get(0));
	}

	/**
	 * 
	 * Get information about the residues associated with a model. This uses the Chimera listr
	 * command. We don't return the resulting residues, but we add the residues to the model.
	 * 
	 * @param model
	 *            the ChimeraModel to get residue information for
	 * 
	 */
	public void addResidues(ChimeraModel model) {
		String modelString = ChimUtils.getModelString(model);
		// Get the list -- it will be in the reply log
		List<String> reply = chimera.sendChimeraCommand("listinfo residues " + model.toSpec(), true);
		if (reply == null) {
			return;
		}
		for (String inputLine : reply) {
			AtomSpec spec = AtomSpec.getListInfoAtomSpec(inputLine, structureManager);
			ChimeraResidue r = new ChimeraResidue(spec);
			if (modelString.equals(ChimUtils.getModelString(r))) {
				model.addResidue(r);
			}
		}
	}

	public List<String> getAttrList() {
		List<String> attributes = new ArrayList<String>();
		final List<String> reply = chimera.sendChimeraCommand("listinfo resattr", true);
		if (reply != null) {
			for (String inputLine : reply) {
				String[] lineParts = inputLine.split("\\s");
				if (lineParts.length == 2 && lineParts[0].equals("resattr")) {
					attributes.add(lineParts[1]);
				}
			}
		}
		return attributes;
	}

	public Map<ChimeraResidue, Object> getAttrValues(String aCommand, ChimeraModel model,
	                                                 boolean useSel) {
		Map<ChimeraResidue, Object> values = new HashMap<ChimeraResidue, Object>();
		String sel = model.toSpec();
		if (useSel)
			sel = "sel";
		final List<String> reply = chimera.sendChimeraCommand("listinfo residue " + sel
				+ " attribute " + aCommand, true);
		if (reply != null) {
			for (String inputLine : reply) {
				String[] lineParts = inputLine.split("\\s");
				// Need to look for both old and new styles of spec
				if (lineParts.length == 5 || lineParts.length == 7) {
					ChimeraResidue residue = ChimUtils.getResidue(lineParts[2], model, this);
					String value = lineParts[4];
					if (residue != null) {
						if (value.equals("None")) {
							continue;
						}
						if (value.equals("True") || value.equals("False")) {
							values.put(residue, Boolean.valueOf(value));
							continue;
						}
						try {
							Double doubleValue = Double.valueOf(value);
							values.put(residue, doubleValue);
						} catch (NumberFormatException ex) {
							values.put(residue, value);
						}
					}
				}
			}
		}
		return values;
	}

	public boolean isChimeraLaunched() {
		return chimera.isChimeraLaunched();
	}

	public boolean launchChimera(List<String> chimeraPaths) {
		// Do nothing if Chimera is already launched
		if (chimera.isChimeraLaunched()) {
			return true;
		}

		return chimera.launchChimera(chimeraPaths);
	}

	public void clearOnChimeraExit() {
		structureManager.clearOnChimeraExit();
	}

	public void exitChimera() {
		chimera.exitChimera();
		currentModelsMap.clear();
		clearOnChimeraExit();
	}

	public StructureManager getStructureManager() {
		return structureManager;
	}

}
