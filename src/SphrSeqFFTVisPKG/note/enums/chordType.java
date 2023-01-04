package SphrSeqFFTVisPKG.note.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * chord type
 * @author John Turner
 *
 */
public enum chordType {
	Major(0),			//1,3,5
	Minor(1),			//1,b3,5
	Augmented(2),		//1,3,#5
	MajFlt5(3),			//1,3,b5
	Diminished(4),		//1,b3,b5
	Sus2(5),			//1,2,5
	Sus4(6),			//1,4,5
	Maj6(7),			//1,3,5,6
	Min6(8),			//1,b3,5,6
	Maj7(9),			//1,3,5,7
	Dom7(10),			//1,3,5,b7
	Min7(11),			//1,b3,5,b7
	Dim7(12),			//1,b3,b5,bb7==6
	None(13);			//not a predifined chord type
	private int value; 
	private static Map<Integer, chordType> map = new HashMap<Integer, chordType>(); 
	static { for (chordType enumV : chordType.values()) { map.put(enumV.value, enumV);}}
	private chordType(int _val){value = _val;} 
	public int getVal(){return value;}
	public static chordType getVal(int idx){return map.get(idx);}
	public static int getNumVals(){return map.size();}					//get # of values in enum	
	/**
	 * return array of halfstep displacements for each note of chord, starting with 0 for root
	 * @param typ
	 * @return
	 */
	public static int[] getChordDisp(chordType typ){
		switch (typ){
		case Major		:{return (new int[]{0,4,7});}	//1,3,5
		case Minor		:{return (new int[]{0,3,7});}	//1,b3,5
		case Augmented	:{return (new int[]{0,4,8});}	//1,3,#5
		case MajFlt5	:{return (new int[]{0,4,6});}	//1,3,b5
		case Diminished	:{return (new int[]{0,3,6});}	//1,b3,b5
		case Sus2       :{return (new int[]{0,2,7});}	//1,2,5
		case Sus4       :{return (new int[]{0,5,7});}	//1,4,5			
		case Maj6       :{return (new int[]{0,4,7,9});}	//1,3,5,6
		case Min6      	:{return (new int[]{0,3,7,9});}	//1,b3,5,6
		case Maj7      	:{return (new int[]{0,4,7,11});}	//1,3,5,7
		case Dom7      	:{return (new int[]{0,4,7,10});}	//1,3,5,b7
		case Min7      	:{return (new int[]{0,3,7,10});}	//1,b3,5,b7
		case Dim7      	:{return (new int[]{0,3,6,9});}	//1,b3,b5,bb7==6
		case None      	:{return (new int[]{0});}	//not a predifined chord type
		default         :{return (new int[]{0});}						
		}			
	}
	
}//enum chordType