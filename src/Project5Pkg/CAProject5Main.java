package Project5Pkg;

import processing.core.PApplet;
public class CAProject5Main {

	public static void main(String[] passedArgs) {		
		String[] appletArgs = new String[] { "Project5Pkg.CAProject5" };
		    if (passedArgs != null) {
		    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
		    } else {
		    	PApplet.main(appletArgs);
		    }
	}//main
}
