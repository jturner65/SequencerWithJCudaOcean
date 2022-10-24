package SphrSeqFFTVisPKG.note.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * note duration types - dotted get mult by 1.5, tuples, get multiplied by (2/tuple size) -> triplets are 2/3 duration (3 in the space of 2)
 * @author 7strb
 *
 */
public	enum durType {
		Whole(1024),Half(512),Quarter(256),Eighth(128),Sixteenth(64),Thirtisecond(32); 
		private int value; 
		private static Map<Integer, durType> map = new HashMap<Integer, durType>(); 
	    static { for (durType enumV : durType.values()) { map.put(enumV.value, enumV);}}
		private durType(int _val){value = _val;} 
		public int getVal(){return value;}
		public static durType getVal(int idx){return map.get(idx);}
		public static int getNumVals(){return map.size();}						//get # of values in enum
}


