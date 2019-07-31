import java.io.*;
import java.util.*;

/**
*Ceci est le code qui lit un labyrinthe à partir d'un fichier de 1 et de 0 séparés par des virgules
*/

public class LectureEcriture{

    private static PrintWriter fW;
    private static Scanner fR;
    private static char mode;

    /**
    *Méthode qui ouvre un fichier contenant une String représentant le labyrinthe
    *@param : nomDuFichier qui est le nom du labyrinthe dans le répertoire "Types_Labyrinthe", s une chaîne de caractères
    */
    public static void ouvrir(String nomDuFichier, String s){
    	try{
    	    mode = (s.toUpperCase()).charAt(0);
    	    if(mode == 'R')
    		fR = new Scanner(new File(nomDuFichier));
    	    else if(mode == 'W')
    		fW = new PrintWriter(new BufferedWriter(new FileWriter(nomDuFichier)));
    	    else
    		System.out.println("Erreur de format");
    	} catch(IOException e){
    	    System.out.println(e);
    	    e.printStackTrace();
    	}
    }

    /**
    *Fonction qui va créer un labyrinthe à partir d'un fichier
    */
    public static ArrayList<ArrayList<Integer>> charger(){
	ArrayList<ArrayList<Integer>> res = new ArrayList<>();
	int i=0;
	int j=0;
	while(fR.hasNextLine()){
	    ArrayList<Integer> tmp = new ArrayList<>();
	    String ligne = fR.nextLine();
	    String t[] = ligne.split(",");
	    for(j=0; j<t.length; j++)
		tmp.add(Integer.parseInt(t[j]));
	    res.add(tmp);
	    i++;
	}
	return res;
    }

    /**
    *Fonction qui ferme le chargement d'un labyrinthe une fois l'opération terminée
    */
    public static void fermer(){
	if(mode == 'R')
	    fR.close();
	else if(mode == 'W')
	    fW.close();
	else
	    System.out.println("Erreur : aucun fichier à fermer");
    }
}
