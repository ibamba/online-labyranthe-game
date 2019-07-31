import java.util.*;

public class Case{
    private static final int MUR = 0;
    private static final int SANS_MUR = 1;
    private static final int FANTOME = 2;
    private static final int JOUEUR = 3;

    private int statut;

    /**
    *Constructeur d'une case
    * @arg s : son statut
    */
    public Case(int s){
	this.statut=s;
    }

    /**
    *Récupération du statut d'une case
    */
    public int getStatut(){
	return statut;
    }

    /**
    *Change le statut d'une case en fonction de s'il y a un fantôme, joueur, mur ou rien
    *@param : stat le nouveau statut
    */
    public void setStatut(int stat){
	this.statut = stat;
    }

    /**
    *Renvoie une représentation textuelle de la case
    */
    public String toString(){
	if(statut==MUR) return "X";
	else if(statut==SANS_MUR) return " ";
	else if(statut==JOUEUR) return "J";
	else return "F";
    }
}
