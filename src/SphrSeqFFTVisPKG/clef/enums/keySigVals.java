package SphrSeqFFTVisPKG.clef.enums;

import java.util.HashMap;
import java.util.Map;

//key signatures
public enum keySigVals {
	CMaj(0),GMaj(1),DMaj(2),Amaj(3),EMaj(4),BMaj(5),FsMaj(6),CsMaj(7),GsMaj(8),DsMaj(9),AsMaj(10),Fmaj(11); 
	private int value; 
	private static final String[] _typeName = new String[]{
		"CMaj - 0 #'s","GMaj - 1 #","DMaj - 2 #'s","Amaj - 3 #'s","EMaj - 4 #'s","BMaj - 5 #'s",
		"FsMaj - 6 #'s","CsMaj - 5 b's","GsMaj - 4 b's","DsMaj - 3 b's","AsMaj - 2 b's","Fmaj - 1 b"};
	
	private static Map<Integer, keySigVals> map = new HashMap<Integer, keySigVals>(); 
    static { for (keySigVals enumV : keySigVals.values()) { map.put(enumV.value, enumV);}}
	private keySigVals(int _val){value = _val;} 
	public int getVal(){return value;} 	
	public static keySigVals getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}						//get # of values in enum
	public String getName() {return _typeName[value];}
	@Override
    public String toString() { return _typeName[value]; }	

}