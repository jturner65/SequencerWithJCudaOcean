package SphrSeqFFTVisPKG.clef.enums;

import java.util.HashMap;
import java.util.Map;

public enum clefType{
	Treble(0), Bass(1), Alto(2), Tenor(3), Piano(4), Drum(5); 
	private int value; 
	private static Map<Integer, clefType> map = new HashMap<Integer, clefType>(); 
    static { for (clefType enumV : clefType.values()) { map.put(enumV.value, enumV);}}
	private clefType(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static clefType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	/**
	 * where is middle c for passed clef type's notes
	 */
	public static float getC4LocMultClef(clefType clef){
		switch (clef){
			case Treble : {return 5;}
			case Bass   : {return -1;}
			case Alto   : {return 2;}
			case Tenor  : {return 2;}
			case Drum   : {return 2;}
			case Piano	: {return 5;}
			default:		break;
		}
		return 0;
	}	
	
	
};	