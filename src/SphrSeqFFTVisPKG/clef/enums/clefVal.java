package SphrSeqFFTVisPKG.clef.enums;

import java.util.HashMap;
import java.util.Map;

public enum clefVal{
	Treble(0), Bass(1), Alto(2), Tenor(3), Piano(4), Drum(5); 
	private int value; 
	private static Map<Integer, clefVal> map = new HashMap<Integer, clefVal>(); 
    static { for (clefVal enumV : clefVal.values()) { map.put(enumV.value, enumV);}}
	private clefVal(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static clefVal getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
};	