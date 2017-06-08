package edu.ucsf.rbvi.structureVizX.internal.model;

import java.lang.Comparable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.structureVizX.internal.utils.StringUtils;

/**
 * This class provides a simple wrapper class for the various parts of a ChimeraX
 * atom spec.  It also serves to centralize all of the atomspec parsing
 * 
 * @author scooter
 * 
 */
public class AtomSpec implements Comparable<AtomSpec> {

	private String modelName; // The possible name of this model
	private int modelNumber; // The model number
	private String[] subModelIds; // sub-model identifiers for this residue

	private String chain;
	private String residueIndex;
	private String residueType;
	private String insertionCode;
	private int residueNumber;
	private String atom;

	/**
	 * Main constructor for ChimeraX atom specs.  In
	 * general, a ChimeraX atom spec looks like:
	 * 	#modelNumber[.subModel]*[/chainId][:residue][@atom]
	 * Note that we do not currently support ranges or booleans
	 */
	public static AtomSpec getChimeraXAtomSpec(String inputString, StructureManager structureManager) {
		AtomSpec spec = new AtomSpec();
		getAtom(inputString, spec, structureManager);
		return spec;
	}

	public static AtomSpec getAtomSpec(ChimeraStructuralObject input) {
		AtomSpec spec = new AtomSpec();
		ChimeraModel model = input.getChimeraModel();
		spec.modelName = model.getModelName();
		spec.modelNumber = model.getModelNumber();
		spec.subModelIds = model.getSubModelIds();
		if (input instanceof ChimeraModel)
			return spec;

		if (input instanceof ChimeraChain) {
			spec.chain = ((ChimeraChain)input).getChainId();
			return spec;
		} else if (input instanceof ChimeraResidue) {
			ChimeraResidue res = (ChimeraResidue)input;
			spec.chain = res.getChainId();
			spec.residueNumber = res.getResidueNumber();
			spec.insertionCode = res.getInsertionCode();
			spec.residueType = res.getType();
			spec.residueIndex = res.getIndex();
		}
		return spec;
	}

	/**
	 * This method takes a Cytoscape attribute specification ([structure[.modelNo]#][residue][.chainID]) where
	 * structure := 4-character code | "URL" | "path" and
	 * creates an atom spec.  Note that this is NOT a ChimeraX atom spec!
	 *
	 * TODO: need to handle multiple residues and residue ranges!
	 */
	public static AtomSpec getAttributeAtomSpec(String attribute, StructureManager structureManager) {
		if (attribute == null || attribute.length() == 0)
			return null;

		AtomSpec spec = new AtomSpec();

		// [pdbID[.modelNo]#][residueID][.chainID]
		// pdbID := 4-character code | "URL" | "path"
		String[] split = attribute.split("#");
		String resChain = null;
		// if no "#" then it is either only a pdb id or a residue or a chain
		if (split.length == 1) {
			// pdb id without model
			if (attribute.length() == 4 && attribute.indexOf("\\.") < 0) {
				parseModelID(attribute, spec, structureManager);
			}
			// pdb link or file
			else if (attribute.startsWith("\"")) {
				parseModelID(attribute, spec, structureManager);
			}
			// chain and residue or model and number
			else {
				String[] splitSplit = attribute.split("\\.");
				if (splitSplit.length == 1) {
					// only a chain or a residue
					resChain = attribute;
				} else {
					try {
						// pdb with a model
						Integer.parseInt(splitSplit[1]);
						parseModelID(attribute, spec, structureManager);
					} catch (NumberFormatException ex) {
						// residue and chain
						resChain = attribute;
					}
				}
			}
		} else if (split.length == 2) {
			// model and residue+chain
			parseModelID(split[0], spec, structureManager);
			resChain = split[1];
		} else {
			// model string with "#"
			// TODO: [Optional] Are there more possibilities?
			parseModelID(attribute.substring(0, attribute.lastIndexOf("#")), spec, structureManager);
			resChain = attribute.substring(attribute.lastIndexOf("#") + 1, attribute.length());
		}
		if (resChain != null) {
			//System.out.println(resChain);
			String[] resChainSplit = resChain.split("\\.");
			if (resChainSplit.length == 1) {
				// TODO: [Optional] Find a better way to distinguish between chain and residue
				// if only one character and not an int, probably a chain
				if (resChainSplit[0].length() == 1) {
					try {
						Integer.parseInt(resChainSplit[0]);
						spec.residueNumber = Integer.valueOf(resChainSplit[0]);
						spec.residueIndex = resChainSplit[0];
					} catch (NumberFormatException ex) {
						spec.chain = resChainSplit[0];
					}
				} else {
					spec.residueNumber = Integer.valueOf(resChainSplit[0]);
					spec.residueIndex = resChainSplit[0];
				}
			} else if (resChainSplit.length == 2) {
				spec.residueType = resChainSplit[0];
				spec.residueNumber = Integer.valueOf(resChainSplit[0]);
				spec.residueIndex = resChainSplit[0];
				spec.chain = resChainSplit[1];
			} else {
				// too many dots?
				structureManager.logError("Could not parse residue identifier: " + attribute);
			}

			if (spec.residueNumber >= 0) {
			}
		}
		return spec;
	}

	/**
	 * Listinfo models returns lines of the form:
	 *   model id #[number] type [type] name [name]
	 */
	public static AtomSpec getListInfoAtomSpec(String listInfo, StructureManager structureManager) {
		AtomSpec spec = new AtomSpec();

		String[] tokens = StringUtils.tokenize(listInfo);

		// First, figure out what kind of list we've got.
		if (tokens[0].equals("residue")) {
			spec.residueType = tokens[4];
			if (tokens.length == 7)
				spec.residueIndex = tokens[6];

			getResidue(tokens[2], spec, structureManager);
		} else if (tokens[0].equals("chain")) {
			getChain(tokens[2], spec, structureManager);
		} else if (tokens[0].equals("model")) {
			getModel(tokens[2], spec, structureManager);
			spec.modelName = tokens[6];
		}
		// System.out.println("Got list info spec: "+spec.toString());
		return spec;
	}

	public static void getAtom(String atomId, AtomSpec spec, StructureManager structureManager) {
		String[] atomSpec = atomId.split("@");
		if (atomSpec.length > 1)
			spec.atom = atomSpec[1];
		getResidue(atomSpec[0], spec, structureManager);
	}

	public static void getResidue(String residueId, AtomSpec spec, StructureManager structureManager) {
		String[] resSpec = residueId.split(":");
		if (resSpec.length > 1) {
			spec.residueIndex = resSpec[1];
			Pattern p = Pattern.compile("(\\d*)([A-Z]?)");
			Matcher m = p.matcher(resSpec[1]);
			if (m.matches()) {
				spec.residueNumber = Integer.valueOf(m.group(1));
				if (m.groupCount() > 1)
					spec.insertionCode = m.group(2);
				else
					spec.insertionCode = null;
			}
		}
		if (residueId.startsWith(":"))
			getChain("/_", spec, structureManager);
		else
			getChain(resSpec[0], spec, structureManager);
	}

	public static void getChain(String chainId, AtomSpec spec, StructureManager structureManager) {
		String[] chainSpec = chainId.split("/");
		if (chainSpec.length > 1)
			spec.chain = chainSpec[1];
		if (chainId.startsWith("/")) {
			// No model id!  We better hope we only have one model
			ChimeraModel model = structureManager.getChimeraManager().getChimeraModel();
			spec.modelName = model.getModelName();
			spec.modelNumber = model.getModelNumber();
			spec.subModelIds = model.getSubModelIds();
		} else
			getModel(chainSpec[0], spec, structureManager);
	}

	public static void getModel(String modelId, AtomSpec spec, StructureManager structureManager) {
		if (modelId.length() == 0) return;
		parseModelID(modelId, spec, structureManager);
	}

	protected static void parseModelID(String modelID, AtomSpec spec, StructureManager structureManager)
	{
		if (modelID.startsWith("\"")) {
			if (modelID.endsWith("\"")) {
				spec.modelName = modelID.substring(1, modelID.length() - 1);
				return;
			} else {
				try {
					Integer.parseInt(modelID.substring(modelID.lastIndexOf("\"") + 2,
							modelID.length()));
					spec.modelName = modelID.substring(0, modelID.lastIndexOf("\"") - 1);
					spec.subModelIds = new String[1];
					spec.subModelIds[0] = modelID.substring(modelID.lastIndexOf("\"") + 2,
							modelID.length());
				} catch (NumberFormatException ex) {
					spec.modelName = modelID.substring(1);
				}
			}
		} else if (modelID.startsWith("#")) {
			modelID = modelID.substring(1);
			spec.modelNumber = Integer.valueOf(modelID);
		} else {
			String[] modelIDNo = modelID.split("\\.");
			if (modelIDNo.length == 1) {
				spec.modelName = modelIDNo[0];
			} else if (modelIDNo.length == 2) {
				try {
					Integer.parseInt(modelIDNo[1]);
					spec.modelName = modelIDNo[0];
					spec.subModelIds = new String[1];
					spec.subModelIds[0] = modelIDNo[1];
				} catch (NumberFormatException ex) {
					spec.modelName = modelID;
				}
			} else {
				// length > 1, so we probably have a file name with "." in it
				structureManager.logInfo("Could not parse model identifier: " + modelID);
				spec.modelName = modelID;
			}
		}
	}

	/**
	 * Return a collapsed spec string, assuming that the
	 * specs array is sorted
	 */
	public static String collapseSpecs(List<AtomSpec> specs) {
		String specCommand = null;
		int modelNumber = -1;
		String modelSubString = null;
		String chainId = null;
		int residueNumber = -1;
		int residueEnd = -1;
		for (AtomSpec spec: specs) {
			if (spec.modelNumber != modelNumber) {
				if (specCommand == null) {
					specCommand = spec.toSpec();
				} else {
					specCommand = endRange(specCommand, residueEnd);
					specCommand += "|"+spec.toSpec();
				}
				modelNumber = spec.modelNumber;
				modelSubString = spec.getSubModelString();
				chainId = spec.chain;
				residueNumber = spec.residueNumber;
				residueEnd = -1;
				continue;
			}

			if (spec.getSubModelString() != modelSubString) {
				specCommand = endRange(specCommand, residueEnd);
				specCommand += "|"+spec.toSpec();
				modelSubString = spec.getSubModelString();
				chainId = spec.chain;
				residueNumber = spec.residueNumber;
				residueEnd = -1;
				continue;
			}

			if (!spec.chain.equals(chainId)) {
				specCommand = endRange(specCommand, residueEnd);
				specCommand += "|"+spec.toSpec();
				chainId = spec.chain;
				residueNumber = spec.residueNumber;
				residueEnd = -1;
				continue;
			}

			if (spec.residueNumber == residueNumber || spec.residueNumber == residueEnd) {
				continue; // duplicate?
			}

			// Are we accumulating a range?
			if (residueEnd > 0) {
				// Yes
				if (spec.residueNumber == residueEnd+1) {
					residueEnd = spec.residueNumber;
					continue;
				} else {
					specCommand = endRange(specCommand, residueEnd);
					residueEnd = -1;
				}
			} else if (residueNumber != -1 && spec.residueNumber == residueNumber+1) {
				residueEnd = spec.residueNumber;
				continue;
			}
			specCommand += ","+spec.residueNumber;
			residueNumber = spec.residueNumber;
		}
		specCommand = endRange(specCommand, residueEnd);
		return specCommand;
	}

	private static String endRange(String specCommand, int residueEnd) {
		if (specCommand == null || residueEnd < 0) return specCommand;
		specCommand += "-"+residueEnd;
		return specCommand;
	}

	protected AtomSpec() {
		modelName = null;
		modelNumber = -1;
		subModelIds = null;
		chain = null;
		residueType = null;
		residueIndex = null;
		residueNumber = -1;
		insertionCode = null;
		atom = null;
	}

	public String toSpec() {
		String spec = "#"+modelNumber+getSubModelString();
		if (chain != null)
			spec += "/"+chain;
		if (residueNumber >= 0) {
			spec += ":"+residueNumber;
			if (insertionCode != null)
				spec += insertionCode;
		}
		if (atom != null)
			spec += "@"+atom;
		return spec;
	}

	public String toString() {
		String str = "AtomSpec: "+toSpec();
		if (modelName != null)
			str += " for "+modelName;
		if (residueType != null)
			str += " residue type "+residueType;
		return str;
	}

	public String getName() {
		return modelName;
	}

	public int getModelNumber() {
		return modelNumber;
	}

	public String[] getSubModelIds() {
		return subModelIds;
	}

	public String getSubModelString() {
		String subString = "";
		if (subModelIds != null && subModelIds.length > 0) {
			for (String sub: subModelIds)
				subString += "."+sub;
		}

		return subString;
	}

	public String getChainId() { return chain; }
	public void setChainId(String chain) { this.chain = chain; }

	public String getResidueType() { return residueType; }
	public void setResidueType(String type) { this.residueType = type; }

	public String getResidueIndex() { return residueIndex; }
	public void setResidueIndex(String index) { residueIndex = index; }

	public int getResidueNumber() { return residueNumber; }
	public void setResidueNumber(int residueNumber) { this.residueNumber = residueNumber; }

	public String getResidueInsertionCode() { return insertionCode; }
	public void setResidueInsertionCode(String code) { insertionCode = code; }

	public String getAtomName() { return atom; }
	public void setAtomName(String atom) { this.atom = atom; }

	@Override
	public int compareTo(AtomSpec o) {
		// Check model numbers
		if (modelNumber != o.modelNumber) {
			if (modelNumber < o.modelNumber) return -1;
			return 1;
		}

		// Check subModel numbers
		if (subModelIds != null || o.subModelIds != null) {
			if (subModelIds == null)
				return 1;
			if (o.subModelIds == null)
				return -1;

			// OK, now we know that both have submodels
			int length;
			int longer;
		 	if (subModelIds.length == o.subModelIds.length) {
				length = subModelIds.length;
				longer = 0;
			} else if (subModelIds.length < o.subModelIds.length) {
				length = subModelIds.length;
				longer = -1;
			} else {
				length = o.subModelIds.length;
				longer = 1;
			}
			for (int i = 0; i < length; i++) {
				String id = subModelIds[i];
				String oid = o.subModelIds[i];

				if (id == oid)
					continue;

				if (id.matches("\\d+")) {
					if (oid.matches("\\d+")) {
						// Both numbers
						int intId = Integer.valueOf(id);
						int intOid = Integer.valueOf(oid);
						if (intId < intOid)
							return -1;
						return 1;
					} else {
						return -1; // Numbers before letters
					}
				} else if (oid.matches("\\d+")) {
					return 1;
				} else {
					return id.compareTo(oid);
				}
			}

			// At this point, we've stepped through all common submodels.
			if (longer != 0) return longer;
		}

		if (!chain.equals(o.chain))
			return chain.compareTo(o.chain);

		if (residueNumber == o.residueNumber)
			return 0;

		if (residueNumber < o.residueNumber)
			return -1;

		// NOTE: We're skipping insertion codes and atoms here
		return 1;
	}

}
