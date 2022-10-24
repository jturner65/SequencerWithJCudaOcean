package SphrSeqFFTVisPKG.note.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * note and key value
 * @author 7strb
 *
 */
public enum nValType {
	C(0),Cs(1),D(2),Ds(3),E(4),F(5),Fs(6),G(7),Gs(8),A(9),As(10),B(11),rest(12); 
	private int value; 
	private static Map<Integer, nValType> map = new HashMap<Integer, nValType>(); 
    static { for (nValType enumV : nValType.values()) { map.put(enumV.value, enumV);}}
	private nValType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static nValType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
}
