package SphrSeqFFTVisPKG.note.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * note and key value
 * @author 7strb
 *
 */
public enum nValType {
	C(0),Cs(1),D(2),Ds(3),E(4),F(5),Fs(6),G(7),Gs(8),A(9),As(10),B(11),rest(12); 
	
	public static final nValType[] wKeyVals = new nValType[] {C, B, A, G, F,E,D};
	public static final nValType[] bKeyVals = new nValType[] {As, Gs, Fs, Ds, Cs};
	private int value; 
	private static Map<Integer, nValType> valMap = new HashMap<Integer, nValType>(); 
	//array of naturals decreased by a sharp in y on grid
	private static Map<nValType, Boolean> hasSharps = new HashMap<nValType, Boolean>();
	//array of naturals decreased by a flat in y on grid
	private static Map<nValType, Boolean> hasFlats = new HashMap<nValType, Boolean>(); 
	//array of all natural notes
	private static Map<nValType, Boolean> isNaturalNotes = new HashMap<nValType, Boolean>(); 
	static { 
	    for (nValType enumV : nValType.values()) {	
	    	valMap.put(enumV.value, enumV);
	    	hasSharps.put(enumV, false);
			hasFlats.put(enumV, false);
			isNaturalNotes.put(enumV, true);
		}
	    for (nValType enumV : bKeyVals) {
	    	isNaturalNotes.put(enumV, false);
	    }	
		isNaturalNotes.put(rest, false);
		hasFlats.put(A, true);
		hasFlats.put(B, true);
		hasFlats.put(D, true);
		hasFlats.put(E, true);
		hasFlats.put(G, true);		
		hasSharps.put(A, true);
		hasSharps.put(C, true);
		hasSharps.put(D, true);
		hasSharps.put(F, true);
		hasSharps.put(G, true);
	}
	private nValType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static nValType getVal(int idx){return valMap.get(idx);}
	public static int getNumVals(){return valMap.size();}						//get # of values in enum
	
	public boolean chkHasSharps(){return hasSharps.get(this);}
	public boolean chkHasFlats(){return hasFlats.get(this);}
	public boolean isNaturalNote(){return isNaturalNotes.get(this);}
	public String getKeyNames(ArrayList<nValType> keyAra){String res = "";for(int i=0;i<keyAra.size();++i){res += "|i:"+i+" : val="+keyAra.get(i); }return res;}	

}//enum nValType
