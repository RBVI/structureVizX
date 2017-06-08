package edu.ucsf.rbvi.structureVizX.internal.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager.ModelType;

public abstract class ChimUtils {

	private static Logger logger = LoggerFactory
			.getLogger(edu.ucsf.rbvi.structureVizX.internal.model.ChimUtils.class);

	static int MAX_SUB_MODELS = 1000;

	public static final HashMap<String, String> aaNames;

	public static String RESIDUE_ATTR = "ChimeraResidue";
	public static String RINALYZER_ATTR = "RINalyzerResidue";
	public static String DEFAULT_STRUCTURE_KEY = "pdbFileName";

	/**
	 * Parse the model number returned by Chimera and return the int value
	 */
	// invoked by the ChimeraModel constructor
	// line = model id #0 type Molecule name 1ert
	public static Object[] parseModelNumber(String inputLine) {
		Object[] results;

		int hash = inputLine.indexOf('#');
		int space = inputLine.indexOf(' ', hash);
		if (space <= 0) // Don't have ' ' in string
			space = inputLine.length();
		String[] submodels = inputLine.substring(hash+1, space).split(".");
		String model = submodels[0];
		if (submodels.length > 1) {
			results = new Object[submodels.length];
			for (int i = 1; i < submodels.length; i++) {
				results[i] = submodels[i];
			}
		} else {
			results = new Object[1];
		}
		Integer modelNumber = null;
		try {
			modelNumber = Integer.parseInt(model);
		} catch (Exception e) {
			logger.warn("Unexpected return from Chimera: " + inputLine, e);
			return null;
		}

		results[0] = modelNumber;
		return results;
	}

	public static Color parseModelColor(String inputLine) {
		try {
			int colorStart = inputLine.indexOf("color ");
			String colorString = inputLine.substring(colorStart + 6);
			// System.out.println("Color: "+colorString);
			String[] rgbStrings = colorString.split(",");
			int[] rgbValues = new int[4];
			for (int i = 0; i < rgbStrings.length; i++) {
				rgbValues[i] = Integer.valueOf(rgbStrings[i]);
			}
			if (rgbStrings.length == 4) {
				return new Color(rgbValues[0], rgbValues[1], rgbValues[2], rgbValues[3]);
			} else {
				return new Color(rgbValues[0], rgbValues[1], rgbValues[2]);
			}
		} catch (Exception ex) {
			logger.warn("Unexpected return from Chimera: " + inputLine, ex);
		}
		return Color.white;
	}

	/**
	 * Create the key to use for forming the model/submodel key into the modelHash
	 * 
	 * @param model
	 *            the model number
	 * @param subModel
	 *            the submodel number
	 * @return the model key as an Integer
	 */
	public static String makeModelKey(int model, String[] subModels) {
		String modelKey = ""+model;
		if (subModels == null || subModels.length == 0) return modelKey;
		for (String sub: subModels)
			modelKey = "."+sub;

		return modelKey;
	}

	// invoked by the getResdiue (parseConnectivityReplies in CreateStructureNetworkTask)
	// atomSpec = #0:1.A or #1:96.B@N
	public static ChimeraModel getModel(String atomSpec, ChimeraManager chimeraManager) {
		// System.out.println("getting model for "+atomSpec);
		AtomSpec spec = AtomSpec.getChimeraXAtomSpec(atomSpec, chimeraManager.getStructureManager());
		return chimeraManager.getChimeraModel(spec);
	}

	// invoked by the parseConnectivityReplies in CreateStructureNetworkTask
	// atomSpec = #0:1.A or #1:96.B@N
	public static ChimeraResidue getResidue(String atomSpec, ChimeraManager chimeraManager) {
		// System.out.println("Getting residue from: "+atomSpec);
		AtomSpec spec = AtomSpec.getChimeraXAtomSpec(atomSpec, chimeraManager.getStructureManager());
		ChimeraModel model = chimeraManager.getChimeraModel(spec);
		if (model == null) {
			model = chimeraManager.getChimeraModel();
		}
		String chain = "_";
		if (spec.getChainId() != null)
			chain = spec.getChainId();
		return model.getResidue(chain, spec.getResidueIndex());
	}

	// invoked by the parseConnectivityReplies in CreateStructureNetworkTask
	// atomSpec = #0:1.A or #1:96.B@N
	public static ChimeraResidue getResidue(String atomSpec, ChimeraModel model, ChimeraManager chimeraManager) {
		AtomSpec spec = AtomSpec.getChimeraXAtomSpec(atomSpec, chimeraManager.getStructureManager());
		return getResidue(spec, model, chimeraManager);
	}

	public static ChimeraResidue getResidue(AtomSpec spec, ChimeraModel model, 
	                                        ChimeraManager chimeraManager) {
		String chain = "_";
		if (spec.getChainId() != null)
			chain = spec.getChainId();
		return model.getResidue(chain, spec.getResidueIndex());
	}

	public static String getAtomName(String atomSpec) {
		String[] split = atomSpec.split("@");
		if (split.length > 1) {
			return split[1];
		}
		return atomSpec;
	}

	public static boolean isBackbone(String atom) {
		if (atom.equals("C") || atom.equals("CA") || atom.equals("N") || atom.equals("O")
				|| atom.equals("H"))
			return true;
		return false;
	}

	public static String getIntSubtype(String node, String atom) {
		String[] split = node.split("#| ");
		String resType = "";
		if (split.length == 2) {
			resType = split[0].trim().toUpperCase();
		} else if (split.length == 3) {
			resType = split[1].trim().toUpperCase();
		}
		if (resType.equalsIgnoreCase("HOH") || resType.equalsIgnoreCase("WAT")) {
			return "water";
		} else if (aaNames.containsKey(resType)) {
			if (atom.equals("C") || atom.equals("CA") || atom.equals("N") || atom.equals("O")
					|| atom.equals("H")) {
				return "mc";
			} else {
				return "sc";
			}
		} else {
			return "other";
		}
	}

	public static List<String> getStructureKeys(CyTable table, CyIdentifiable cyObj,
			List<String> attrsFound, StructureManager manager) {
		CyRow row = table.getRow(cyObj.getSUID());
		List<String> cellList = new ArrayList<String>();
		// iterate over attributes that contain structures
		for (String column : attrsFound) {
			CyColumn col = table.getColumn(column);
			if (col == null) {
				continue;
			}

			Class<?> colType = col.getType();
			if (colType == String.class) {
				String cell = row.get(column, String.class, "").trim();
				if (cell == null || cell.equals("")) {
					continue;
				}
				// TODO: [Bug] Will break parsing if residueID contains commas
				String[] cellArray = cell.split(",");
				for (String str : cellArray) {
					AtomSpec spec = AtomSpec.getAttributeAtomSpec(str.trim(), manager);
					if (spec.getName() != null) {
						cellList.add(spec.getName());
					}
				}
			} else if (colType == List.class && col.getListElementType() == String.class) {
				List<String> values = row.getList(column, String.class);
				if (values == null) {
					continue;
				} else {
					for (String str : values) {
						AtomSpec spec = AtomSpec.getAttributeAtomSpec(str.trim(), manager);
						if (spec != null && spec.getName() != null) {
							cellList.add(spec.getName());
						}
					}
				}
			} else {
				continue;
			}
		}
		return cellList;
	}

	public static List<String> getResidueKeys(CyTable table, CyIdentifiable cyObj,
			List<String> attrsFound) {
		CyRow row = table.getRow(cyObj.getSUID());
		List<String> cellList = new ArrayList<String>();
		// iterate over attributes that contain structures
		for (String column : attrsFound) {
			CyColumn col = table.getColumn(column);
			if (col == null) {
				continue;
			}
			Class<?> colType = col.getType();
			if (colType == String.class) {
				String cell = row.get(column, String.class, "").trim();
				if (cell == null || cell.equals("")) {
					continue;
				}
				// TODO: [Bug] Will break parsing if residueID contains commas
				String[] cellArray = cell.split(",");
				for (String str : cellArray) {
					if (!str.trim().equals("")) {
						cellList.add(str.trim());
					}
				}
			} else if (colType == List.class && col.getListElementType() == String.class) {
				List<String> values = row.getList(column, String.class);
				if (values == null) {
					continue;
				} else {
					for (String str : row.getList(column, String.class)) {
						if (!str.trim().equals("")) {
							cellList.add(str.trim());
						}
					}
				}
			} else {
				continue;
			}
		}
		return cellList;
	}

	/**
	 * This method takes a Cytoscape attribute specification ([structure#][residue][.chainID]) and
	 * returns the lowest-level object referenced by the spec. For example, if the spec is "1tkk",
	 * this method will return a ChimeraModel. If the spec is ".A", it will return a ChimeraChain,
	 * etc.
	 * 
	 * @param attrSpec
	 *            the specification string
	 * @param chimeraManager
	 *            the Chimera object we're currently using
	 * @return a ChimeraStructuralObject of the lowest type
	 */
	public static ChimeraStructuralObject fromAttributeOld(String attrSpec,
			ChimeraManager chimeraManager) {
		if (attrSpec == null || attrSpec.indexOf(',') > 0 || attrSpec.indexOf('-') > 0) {
			// No support for either lists or ranges
			logger.warn("No support for identifier: " + attrSpec);
			return null;
		}

		String residue = null;
		String model = null;
		String chain = null;

		ChimeraModel chimeraModel = null;
		ChimeraChain chimeraChain = null;
		ChimeraResidue chimeraResidue = null;

		try {
			String[] split = attrSpec.split("#");
			String resChain = null;
			if (split.length == 1) {
				// no model
				resChain = split[0];
			} else if (split.length == 2) {
				// model and rest
				model = split[0];
				resChain = split[1];
			} else {
				// model string with "#"
				model = attrSpec.substring(0, attrSpec.lastIndexOf("#"));
				resChain = attrSpec.substring(attrSpec.lastIndexOf("#") + 1, attrSpec.length());
			}
			if (resChain != null) {
				String[] resChainSplit = resChain.split("\\.");
				if (resChainSplit.length == 1) {
					residue = resChainSplit[0];
				} else if (resChainSplit.length == 2) {
					residue = resChainSplit[0];
					chain = resChainSplit[1];
				} else {
					// too many dots?
					logger.warn("No support for identifier: " + attrSpec);
				}
			}

			// if (split.length == 1) {
			// // No model
			// residue = split[0];
			// } else if (split.length == 3) {
			// // We have all three
			// model = split[0];
			// residue = split[1];
			// chain = split[2];
			// } else if (split.length == 2 && attrSpec.indexOf('#') > 0) {
			// // Model and Residue
			// model = split[0];
			// residue = split[1];
			// } else {
			// // Residue and Chain
			// residue = split[0];
			// chain = split[1];
			// }

			// System.out.println("model = " + model + " chain = " + chain + " residue = " + residue);
			if (model != null) {
				List<ChimeraModel> models = chimeraManager.getChimeraModels(model,
						ModelType.PDB_MODEL);
				if (models.size() == 1) {
					chimeraModel = models.get(0);
				} else {
					try {
						chimeraModel = chimeraManager.getChimeraModel(Integer.valueOf(model), null);
					} catch (NumberFormatException ex) {
						// ignore
					}
				}
			}
			if (chimeraModel == null) {
				chimeraModel = chimeraManager.getChimeraModel();
			}
			// System.out.println("ChimeraModel = " + chimeraModel);

			if (chain != null) {
				chimeraChain = chimeraModel.getChain(chain);
				// System.out.println("ChimeraChain = " + chimeraChain);
			}
			if (residue != null) {
				if (chimeraChain != null) {
					chimeraResidue = chimeraChain.getResidue(residue);
				} else {
					chimeraResidue = chimeraModel.getResidue("_", residue);
				}
				// System.out.println("ChimeraResidue = " + chimeraResidue);
			}

			if (chimeraResidue != null)
				return chimeraResidue;

			if (chimeraChain != null)
				return chimeraChain;

			if (chimeraModel != null)
				return chimeraModel;

		} catch (Exception ex) {
			logger.warn("Could not parse residue identifier: " + attrSpec, ex);
		}
		return null;
	}

	public static ChimeraStructuralObject fromAttribute(String attrSpec, ChimeraManager chimeraManager) {
		// TODO: Make sure it is OK to remove this: || attrSpec.indexOf('-') > 0
		if (attrSpec == null || attrSpec.indexOf(',') > 0) {
			// No support for either lists or ranges
			// System.out.println("No support for identifier: " + attrSpec);
			logger.warn("No support for identifier: " + attrSpec);
			return null;
		}

		AtomSpec spec = AtomSpec.getAttributeAtomSpec(attrSpec, chimeraManager.getStructureManager());

		ChimeraModel chimeraModel = null;
		ChimeraChain chimeraChain = null;
		ChimeraResidue chimeraResidue = null;

		if (spec.getName() != null) {
			List<ChimeraModel> models = chimeraManager.getChimeraModels(spec.getName(), ModelType.PDB_MODEL);
			if (models.size() == 1) { // usual case with only one model
				chimeraModel = models.get(0);
			} else if (models.size() > 1 && spec.getSubModelIds() != null && spec.getSubModelIds().length > 0) {
				for (ChimeraModel model: models) {
					if (model.getSubModelIds()[0].equals(spec.getSubModelIds()[0])) {
						chimeraModel = model;
						break;
					}
				}
			} else {
				// TODO: [Optional] What is this doing?
				try {
					chimeraModel = chimeraManager.getChimeraModel(Integer.valueOf(spec.getName()), null);
				} catch (NumberFormatException ex) {
					// ignore
				}
			}
		}

		if (chimeraModel == null) {
			chimeraManager.getStructureManager().logError("Incorrect atom spec: '"+attrSpec+"'-- no model!");
			return null;
		}
		if (spec.getChainId() != null)
			chimeraChain = chimeraModel.getChain(spec.getChainId());

		if (spec.getResidueNumber() >= 0) {
			String residue = spec.getResidueIndex();
			if (chimeraChain != null) {
				chimeraResidue = chimeraChain.getResidue(residue);
			} else if (chimeraModel.getChain("_") != null) {
				chimeraResidue = chimeraModel.getResidue("_", residue);
			} else if (chimeraModel.getChainCount() == 1) {
				chimeraResidue = chimeraModel.getResidue(chimeraModel.getChainNames()
						.iterator().next(), residue);
			}
		}

		if (chimeraResidue != null)
			return chimeraResidue;

		if (chimeraChain != null)
			return chimeraChain;

		return chimeraModel;
	}

	/**
	 * Search for structure references in the residue list
	 * 
	 * @param residueList
	 *            the list of residues
	 * @return a concatenated list of structures encoded in the list
	 */
	public static String findStructures(String residueList) {
		if (residueList == null)
			return null;
		String[] residues = residueList.split(",");
		Map<String, String> structureNameMap = new HashMap<String, String>();
		for (int i = 0; i < residues.length; i++) {
			String[] components = residues[i].split("#");
			if (components.length > 1) {
				structureNameMap.put(components[0], components[1]);
			}
		}
		if (structureNameMap.isEmpty())
			return null;

		String structure = null;
		for (String struct : structureNameMap.keySet()) {
			if (structure == null)
				structure = new String();
			else
				structure = structure.concat(",");
			structure = structure.concat(struct);
		}
		return structure;
	}

	// invoked by openStructures in StructureManager
	public static List<String> parseFuncRes(List<String> residueNames, String modelName) {
		List<String> resRanges = new ArrayList<String>();
		for (int i = 0; i < residueNames.size(); i++) {
			String residue = residueNames.get(i);
			// Parse out the structure, if there is one
			String[] components = residue.split("#");
			if (components.length > 1 && !modelName.equals(components[0])) {
				continue;
			} else if (components.length > 1) {
				residue = components[1];
			} else if (components.length == 1) {
				residue = components[0];
			}
			// Check to see if we have a range-spec
			String resRange = "";
			if (residue == null || residue.equals("") || residue.length() == 0) {
				continue;
			}
			String[] range = residue.split("-", 2);
			String chain = null;
			for (int res = 0; res < range.length; res++) {
				if (res == 1) {
					resRange = resRange.concat("-");
					if (chain != null && range[res].indexOf('.') == -1)
						range[res] = range[res].concat("." + chain);
				}

				if (res == 0 && range.length >= 2 && range[res].indexOf('.') > 0) {
					// This is a range spec with the leading residue containing a chain spec
					String[] resChain = range[res].split("\\.");
					chain = resChain[1];
					range[res] = resChain[0];
				}
				// Fix weird SFLD syntax...
				if (range[res].indexOf('|') > 0 && Character.isDigit(range[res].charAt(0))) {
					int offset = range[res].indexOf('|');
					String str = range[res].substring(offset + 1) + range[res].substring(0, offset);
					range[res] = str;
				}

				// Convert to legal atom-spec
				if (Character.isDigit(range[res].charAt(0))) {
					resRange = resRange.concat(range[res]);
				} else if (Character.isDigit(range[res].charAt(1))) {
					resRange = resRange.concat(range[res].substring(1));
				} else if (range[res].charAt(0) == '.') {
					// Do we have a chain spec?
					resRange = resRange.concat(range[res]);
				} else {
					resRange = resRange.concat(range[res].substring(3));
				}
			}
			if (!resRanges.contains(resRange)) {
				resRanges.add(resRange);
			}
		}
		return resRanges;
	}

	static {
		aaNames = new HashMap<String, String>();
		aaNames.put("ALA", "A Ala Alanine N[C@@H](C)C(O)=O");
		aaNames.put("ARG", "R Arg Arginine N[C@@H](CCCNC(N)=N)C(O)=O");
		aaNames.put("ASN", "N Asn Asparagine N[C@@H](CC(N)=O)C(O)=O");
		aaNames.put("ASP", "D Asp Aspartic_acid N[C@@H](CC(O)=O)C(O)=O");
		aaNames.put("CYS", "C Cys Cysteine N[C@@H](CS)C(O)=O");
		aaNames.put("GLN", "Q Gln Glutamine N[C@H](C(O)=O)CCC(N)=O");
		aaNames.put("GLU", "E Glu Glumatic_acid N[C@H](C(O)=O)CCC(O)=O");
		aaNames.put("GLY", "G Gly Glycine NCC(O)=O");
		aaNames.put("HIS", "H His Histidine N[C@@H](CC1=CN=CN1)C(O)=O");
		aaNames.put("ILE", "I Ile Isoleucine N[C@]([C@H](C)CC)([H])C(O)=O");
		aaNames.put("LEU", "L Leu Leucine N[C@](CC(C)C)([H])C(O)=O");
		aaNames.put("LYS", "K Lys Lysine N[C@](CCCCN)([H])C(O)=O");
		aaNames.put("DLY", "K Dly D-Lysine NCCCC[C@@H](N)C(O)=O");
		aaNames.put("MET", "M Met Methionine N[C@](CCSC)([H])C(O)=O");
		aaNames.put("PHE", "F Phe Phenylalanine N[C@](CC1=CC=CC=C1)([H])C(O)=O");
		aaNames.put("PRO", "P Pro Proline OC([C@@]1([H])NCCC1)=O");
		aaNames.put("SER", "S Ser Serine OC[C@](C(O)=O)([H])N");
		aaNames.put("THR", "T Thr Threonine O[C@H](C)[C@](C(O)=O)([H])N");
		aaNames.put("TRP", "W Trp Tryptophan N[C@@]([H])(CC1=CN([H])C2=C1C=CC=C2)C(O)=O");
		aaNames.put("TYR", "Y Tyr Tyrosine N[C@@](C(O)=O)([H])CC1=CC=C(O)C=C1");
		aaNames.put("VAL", "V Val Valine N[C@@](C(O)=O)([H])C(C)C");
		aaNames.put("ASX", "B Asx Aspartic_acid_or_Asparagine");
		aaNames.put("GLX", "Z Glx Glutamine_or_Glutamic_acid");
		aaNames.put("XAA", "X Xaa Any_or_unknown_amino_acid");
		aaNames.put("HOH", "HOH HOH Water [H]O[H]");
	}

	/**
	 * Convert the amino acid type to a full name
	 * 
	 * @param aaType
	 *            the residue type to convert
	 * @return the full name of the residue
	 */
	public static String toFullName(String aaType) {
		if (!aaNames.containsKey(aaType))
			return aaType;
		String[] ids = ((String) aaNames.get(aaType)).split(" ");
		return ids[2].replace('_', ' ');
	}

	/**
	 * Convert the amino acid type to a single letter
	 * 
	 * @param aaType
	 *            the residue type to convert
	 * @return the single letter representation of the residue
	 */
	public static String toSingleLetter(String aaType) {
		if (!aaNames.containsKey(aaType))
			return aaType;
		String[] ids = ((String) aaNames.get(aaType)).split(" ");
		return ids[0];
	}

	/**
	 * Convert the amino acid type to three letters
	 * 
	 * @param aaType
	 *            the residue type to convert
	 * @return the three letter representation of the residue
	 */
	public static String toThreeLetter(String aaType) {
		if (!aaNames.containsKey(aaType))
			return aaType;
		String[] ids = ((String) aaNames.get(aaType)).split(" ");
		return ids[1];
	}

	/**
	 * Convert the amino acid type to its SMILES string
	 * 
	 * @param aaType
	 *            the residue type to convert
	 * @return the SMILES representation of the residue
	 */
	public static String toSMILES(String aaType) {
		if (!aaNames.containsKey(aaType))
			return null;
		String[] ids = ((String) aaNames.get(aaType)).split(" ");
		if (ids.length < 4)
			return null;
		return ids[3];
	}

	public static String getAlignName(ChimeraStructuralObject chimObj) {
		String name = chimObj.getChimeraModel().toString();
		if (chimObj instanceof ChimeraChain) {
			name = ((ChimeraChain) chimObj).toString() + " [" + name + "]";
		}
		return name;
	}

	public static String getModelString(ChimeraStructuralObject chimObj) {
		String str = ""+chimObj.getModelNumber();
		String[] subModelIds = chimObj.getSubModelIds();
		if (subModelIds != null && subModelIds.length > 0) {
			for (String sub: subModelIds) {
				str += "."+sub;
			}
		}
		return str;
	}

}
