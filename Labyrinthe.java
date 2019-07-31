import java.util.*;

/**
*Ceci est le code qui sert à représenter un labyrinthe
*/

public class Labyrinthe{
    private static final int MUR = 0;
    private static final int SANS_MUR = 1;
    private static final int FANTOME = 2;
    private static final int JOUEUR = 3;

    private int height;
    private int width;
    private Case [][] plateau;

    public Labyrinthe(String filePath){
	/**
	   Constructeur du labyrinthe.
	   Génère un labyrinthe aleatoirement
	   @arg h : la hauteur du labyrinthe
	   @arg w : la largeur du labyrinthe
	   @arg plateau : la plate-forme du labyrinthe
	*/
	LectureEcriture.ouvrir("Types_Labyrinthe/"+filePath, "R");
	ArrayList<ArrayList<Integer>> liste = LectureEcriture.charger();
	LectureEcriture.fermer();
	height = liste.get(0).size();
	width = liste.size();
	plateau = new Case[height][width];
	for(int i=0; i<height; i++){
	    for(int j=0; j<width; j++)
		plateau[i][j] = new Case(liste.get(j).get(i));
	}
    }

    /**
    *Retourne la case demandée
    *@param : x et y ses coordonnées
    */
    public Case getCase(int x, int y){
	return plateau[x][y];
    }

    /**
    *Retourne la hauteur d'un Labyrinthe
    */
    public int getHeight(){
    	return height;
    }

    /**
    *Retourne la largeur d'un labyrinthe
    */
    public int getWidth(){
    	return width;
    }

    /**
    *Produit dans le terminal une représentation textuelle du labyrinthe
    */
    public void afficheLabyrinthe(){
	System.out.print(" ");
	for(int i=0; i<height; i++)
	    System.out.print("_");
	System.out.println();
	for(int i=0; i<width; i++){
	    System.out.print("|");
	    for(int j=0; j<height; j++){
		System.out.print(plateau[j][i].toString());
	    }
	    System.out.println("|");
	}
	System.out.print(" ");
	for(int j=0; j<height; j++)
	    System.out.print("T");
	System.out.println();
    }
}
