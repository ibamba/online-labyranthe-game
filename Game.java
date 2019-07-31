import java.io.*;
import java.util.*;
import java.math.*;
import java.net.*;

/**
*Ceci est le code d'une partie de LABYRHANTE
*/

public class Game{

    private static final int LEFT = 0;
    private static final int UP = 1;
    private static final int RIGHT = 2;
    private static final int DOWN = 3;

    private final int port;
    private Labyrinthe lab;
    private String ip;
    private HashMap<String, Boolean> players;
    private ArrayList<int[]> coordPlayers;
    private ArrayList<Integer> scores;
    private int [][] fant;
    private int [] etatFantomes;
    private boolean begun, end, diff;
    private int nbFantomes;
    private InetSocketAddress ia;
    private int lastMoved, gameMode=0, juggernaut;
    private String nomJugger;

    /**
    *Constructeur d'une partie
    *@param : ip l'ip de multidiffusion de la partie, le port UDP sur lequel elle va diffuser ses messages, lab qui est un labyrinthe
    */
    public Game(String ip, int port, Labyrinthe lab){
	this.ip=ip;
	this.port=port;
	this.lab=lab;
	this.players=new HashMap<String, Boolean>();
	nbFantomes=(lab.getWidth() + lab.getHeight())/2;
	fant = new int[nbFantomes][2];
	etatFantomes = new int [nbFantomes];
	this.begun=false;
	this.end=false;
	this.diff=false;
	coordPlayers = new ArrayList<>();
	scores = new ArrayList<>();
	ia = new InetSocketAddress(ip, port);
	for(int i=0; i<etatFantomes.length; i++){
	    etatFantomes[i] = 1; //Présence du fantome
	}
    }

    /**
    *Renvoi l'entier correspondant à un mode de jeu (0 classique, 1 Stealing, 2 Juggernaut)
    */
    public int getMode(){
	return gameMode;
    }

    /**
    *Choix du mode de jeu
    *@param : i un entier correspondant aux possibles modes de jeu
    */
    public void setMode(int i){
	gameMode = i;
    }

    /**
    *Récupérer le nom du mode de jeu en fonction de l'attribut gameMode
    */
    public synchronized String gameModeToString(){
	switch(gameMode){
        case 0:
	    return "CLASSIC";
	case 1:
	    return "STEALING";
	case 2:
	    return "JUGGERNAUT";
	default: return "CLASSIC";
	}
    }

    /**
    *Ajout d'un joueur à la partie (suites à un NEW ou REG), on met un deuxième attribut d'office à false indiquant qu'il n'est pas près à jouer (changeable en faisant START***)
    *@param : nom le pseudo du joueur
    */
    public synchronized void ajouterPlayer(String nom){
	players.put(nom,false);
	placePlayer();
    }

    /**
    *Récupérer l'id d'un joueur dans la partie en fonction de son nom
    *@param : nom le pseudo du joueur
    */
    public int idToIndice(String nom){
	int res=-1;
	for(Map.Entry j : players.entrySet()){
	    res++;
	    if(((String)j.getKey()).equals(nom)) return res;
	}
	return -1;
    }

    /**
    *Suppression d'un joueur de la partie
    @param : nom le pseudo du joueur à supprimer
    */
    public synchronized boolean delete(String nom){
	int ind = idToIndice(nom);
	if(ind != (-1)){
	    players.remove(nom);
	    if(!coordPlayers.isEmpty()){
		coordPlayers.remove(ind);
		scores.remove(ind);
	    }
	    return true;
	}
	return false;
    }

    /**
    *Récupère la position d'un joueur dans le labyrinthe
    *@param : nom le pseudo du joueur dont on veut la position
    */
    public synchronized int [] getPos(String nom){
    	int ind = idToIndice(nom);
    	if(ind != (-1)){
    	    return coordPlayers.get(ind);
    	}
    	int tab [] = {-1,-1};
    	return tab;
    }

    /**
    *Surcharge de la fonction précédente pour avoir la position d'un fantôme
    *@param : i, l'id du fantôme en question dans le tableau de coordonnées des fantômes
    */
    public synchronized int [] getPos(int i){
	return fant[i];
    }

    /**
    *Fonction qui print le labyrinthe, les joueurs, les fantômes côté serveur
    */
    public void affiche(){
	lab.afficheLabyrinthe();
    }

    /**
    *Renvoie les dimensions du labyrinthe
    */
    public int[] getLabTaille(){
	int [] tab = new int[2];
	tab[0] = lab.getHeight();
	tab[1] = lab.getWidth();
	return tab;
    }

    /**
    *Renvoi le nombre de fantômes dans le labyrinthe
    */
    public synchronized int getNbFantomes(){
	return nbFantomes;
    }

    /**
    *Renvoi true si la partie est finie, false sinon
    */
    public synchronized boolean getEnd(){
	return end;
    }

    /**
    *Change l'état de la partie si elle est finie en mettant à true
    */
    public synchronized void setEnd(){
	end=true;
    }

    /**
    *Renvoi l'attribut diff (utilisé dans PlayerService)
    */
    public boolean getDiff(){
      return diff;
    }

    /**
    *Change la valeur de diff
    */
    public void setDiff(){
      diff=true;
    }

    /**
    *Retourne le nombre de joueurs présents dans la partie
    */
    public synchronized int getNbJoueurs(){
	     return players.size();
    }

    /**
    *Renvoie un tableau composé des pseudos des joueurs
    */
    public synchronized String[] getJoueurs(){
    	String [] res = new String[getNbJoueurs()];
    	int i=0;
    	for(Map.Entry e : players.entrySet()){
    	    res[i++] = (String)e.getKey();
    	}
    	return res;
    }

    /**
    *Retourne l'adresse de multidiffusion de la partie
    */
    public String getIp(){
	return ip;
    }

    /**
    *Retourne une InetSocketAddress de la partie elle-même
    */
    public InetSocketAddress getInet(){
	return ia;
    }

    /**
    *Retourne le port de multidiffusion de la partie
    */
    public int getPort(){
	return port;
    }

    /**
    *Retourne les points d'un joueur en fonction de son pseudo
    *@param : nom le pseudo du joueur
    */
    public int getPoints(String nom){
	return scores.get(idToIndice(nom));
    }

    /**
    *Renvoi true si la partie à commencée, false sinon
    */
    public synchronized boolean isBegun(){
	return begun;
    }

    /**
    *Change le statut de la partie quand elle commence
    *et place les fantomes dans le labyrinthe
    */
    public synchronized void setBegun(){
	if(!begun){
	    begun=true;
	    moveGhosts();
	    if(gameMode==2){
		juggernaut=(int)(Math.random() * (getNbJoueurs()));
		nomJugger=getJoueurs()[juggernaut];
	    }
	}
    }

    /**
    *Change le statut d'un joueur en le mettant à "prêt"
    */
    public void isReady(String nom){
	players.replace(nom, true);
    }

    /**
    *Permet de savoir si un joueur est encore vivant ou non dans le
    *mode juggernaut
    *@param nom : le pseudo du joueur
    */
    public boolean live(String nom){
	return players.get(nom);
    }

    /**
    *Vérifie si la partie peut commencer (tout les joueurs sont prêts)
    */
    public boolean canStart(){
    	for(Map.Entry i : players.entrySet()){
    	    if(!(boolean)i.getValue()){
    		return false;
    	    }
    	}
    	return true;
    }

    /**
    *Place un joueur aléatoirement dans le labyrinthe
    */
    public void placePlayer(){
	boolean flag=false;
	int coordx;
	int coordy;
	while(!flag){
	    coordy = (int)(Math.random() * (lab.getWidth()));
	    coordx = (int)(Math.random() * (lab.getHeight()));
	    if(lab.getCase(coordx,coordy).getStatut() == 1){
		int tab [] = {coordx,coordy};
		coordPlayers.add(tab);
		scores.add(0);
		flag = true;
		lab.getCase(coordx,coordy).setStatut(3);
	    }
	}
    }

    /**
    *Bouge le joueur qui a demandé de faire un déplacement
    *@param : str le pseudo du joueur, dist la distance à parcourir, dir la direction vers laquelle il veut aller
    */
    public synchronized int[] movePlayer(String str, int dist, int dir){
    	int id=idToIndice(str);
    	int tmp=0;
    	int prevY = coordPlayers.get(id)[1];
    	int prevX = coordPlayers.get(id)[0];
    	int newX = prevX;
    	int newY = prevY;
    	int tab [] = {prevX, prevY, -1, 0};
	boolean flag=false;
	//tab[0] : coord x du joueur, tab[1] : coord y
	//tab[2] : 0 si le joueur bouffe un fantome
	//         1 si le joueur vole les points d'un autre joueur dans le mode stealing
	//         2 muvement du juggernaut ou décès d'un joueur dans le mode juggernaut
	//         -1 sinon
	//tab[3] : les points du joueur
	//         l'indice du joueur bouffé dans le mode juggernaut ou -1 si le juggernaut se deplace sans bouffer

	juggernaut=idToIndice(nomJugger);

	//supression du joueur de sa case prec
	if(gameMode==2){
	    for(int i=0; i<fant.length; i++){
		if(fant[i][0]==prevX && fant[i][1]==prevY){
		    lab.getCase(prevX, prevY).setStatut(2);
		    flag=true;
		    break;
		}
	    }
	}
	if(!flag && (!otherPlayerOnCase(id, prevX, prevY) || gameMode==2))
	    lab.getCase(prevX, prevY).setStatut(1);

	//détermination de la case max où le joueur peut se déplacer
    	while(tmp <= dist && newX>=0 && newY>=0 && newY<lab.getWidth() && newX<lab.getHeight() && lab.getCase(newX,newY).getStatut() != 0 && scores.get(id)!=-1){
    	    tmp++;
    	    switch(dir){
    	    case LEFT:
    		prevX=newX;
    		newX--;
    		break;
    	    case RIGHT:
    		prevX=newX;
    		newX++;
    		break;
    	    case UP:
    		prevY=newY;
    		newY--;
    		break;
    	    case DOWN:
    		prevY=newY;
    		newY++;
    		break;
    	    default:
    		break;
    	    }
    	}

	//Dans le mode STEALING(1), le dernier joueur arrivé sur la case
	//vole les points du joueur présent avant
	if(gameMode==1 && lab.getCase(prevX, prevY).getStatut()==3){
	    capturedPlayerFants(id, prevX, prevY);
	    tab[2]=1;
	    tab[3]=scores.get(id);
	}

	//mis à jour des coordonnées du joueur
    	tab[0] = prevX;
    	tab[1] = prevY;
	int []_tab={prevX, prevY};
	coordPlayers.set(id, _tab);

	//Dans le mode JUGGERNAUT(2), le juggernaut bouffe le joueur qui est dans la
	//même case que lui et celui-ci est éliminée de la partie
	if(gameMode == 2){
	    if(juggernaut == id){
		tab[2]=2;
		tab[3]=-1;
	    }
	    int xj = coordPlayers.get(juggernaut)[0];
	    int yj = coordPlayers.get(juggernaut)[1];
	    int x,y;
	    for(int i=0; i<coordPlayers.size(); i++){
		x = coordPlayers.get(i)[0];
		y = coordPlayers.get(i)[1];
		if(xj==x && yj == y && juggernaut!=i && scores.get(i)!=-1){
		    scores.set(juggernaut, scores.get(juggernaut)+scores.get(i)); //le score du juggernaut est mis à jour
		    scores.set(i, -1); //Le score du joueur capturé par le juggernaut est mis à -1
		    players.replace(getJoueurs()[i], false); //le joueur est éliminé de la partie
		    tab[2]=2;
		    tab[3]=i; //le joueur capturé
		    break;
		}
	    }
	}

	//Chaque fantome capturée par un joueur lui raaporte 100 points
	if(lab.getCase(prevX, prevY).getStatut()==2 && (gameMode != 2 || id != juggernaut)){
	    scores.set(id, scores.get(id)+100);
	    tab[2]=0;
	    tab[3]=scores.get(id);
	    capturedGhosts(id);
	}
	lab.getCase(prevX, prevY).setStatut(3);

	//A chaque deplacement de joueur, un fantome se deplace
	int idfant;
	do{
	    idfant = (int)(Math.random() * (fant.length));
	}while(etatFantomes[idfant] != 1 && nbFantomes>0);
	moveOneGhost(idfant);

	return tab;
    }

    /**
    *On vérifie si un joueur à été capturé par un autre
    *@param : ind l'id du joueur dans la liste, x la position en abscisse, y la position en ordonnées
    */
    public synchronized void capturedPlayerFants(int ind, int x, int y){
	for(int i=0;i<coordPlayers.size();i++){
    	    int _x = coordPlayers.get(i)[0];
    	    int _y = coordPlayers.get(i)[1];
    	    if(x==_x && y==_y){
		scores.set(ind, scores.get(ind)+scores.get(i));
		scores.set(i, 0);
		return;
    	    }
    	}
    }

    /**
    *On vérifie si un joueur est déjà sur la case sur laquelle un joueur veut bouger
    *@param : ind l'id du joueur dans la liste, x la position en abscisse, y la position en ordonnées
    */
    public boolean otherPlayerOnCase(int ind, int x, int y){
	for(int i=0; i<coordPlayers.size(); i++){
    	    int _x = coordPlayers.get(i)[0];
    	    int _y = coordPlayers.get(i)[1];
    	    if(x==_x && y==_y && ind!=i)
		return true;
	}
	return false;
    }

    /**
    *On vérifie si un joueur va capturer un fantôme suites à son déplacement
    *@param : j l'id du joueur dans la liste
    */
    public synchronized void capturedGhosts(int j){
	for(int i=0;i<fant.length;i++){
	    int x = coordPlayers.get(j)[0];
	    int y = coordPlayers.get(j)[1];
	    if(fant[i][0] == x && fant[i][1] == y){
		etatFantomes[i] = 0;
		fant[i][0] = -1;
		fant[i][1] = -1;
		nbFantomes--;
		break;
	    }
	}
    }

    /**
    *Retourne l'id du joueur étant juggernaut
    */
    public int getJuggernaut(){
	return juggernaut;
    }

    /**
    *Retourne le pseudo du joueur étant juggernaut
    */
    public String getNomJuggernaut(){
	return nomJugger;
    }

    /**
    *Retourne les points du juggernaut
    */
    public int getPointsJuggernaut(){
	return scores.get(juggernaut);
    }

    /**
    *Déplacement des fantômes
    */
    public synchronized void moveGhosts(){
	for(int i=0; i<fant.length; i++){
	    moveOneGhost(i);
	}
    }

    /**
    *Déplacement d'un fantôme en particulier
    *@param : i l'id du fantôme dans la liste des fantômes
    */
    public synchronized void moveOneGhost(int i){
	boolean flag;
	int coordx;
	int coordy;
	reinitialiseFantome(i);
	if(etatFantomes[i] == 1){
	    flag = false;
	    //Choix d'une case aléatoire vide pour accueillir le fantôme
	    while(!flag){
		coordy = (int)(Math.random() * (lab.getWidth()));
		coordx = (int)(Math.random() * (lab.getHeight()));
		if(lab.getCase(coordx,coordy).getStatut() == 1){
		    fant[i][0] = coordx;
		    fant[i][1] = coordy;
		    flag = true;
		    lab.getCase(coordx,coordy).setStatut(2);
		}
	    }
	}
	lastMoved=i;
    }

    /**
    *Réinitialise le labyrinthe après un déplacement de fantôme
    */
    public synchronized void reinitialiseFantome(int i){
	if(etatFantomes[i] == 1){
	    toEmptyCase(fant[i][0], fant[i][1]);
	}
    }

    /**
    *Retourne l'id du dernier fantôme ayant bougé
    */
    public int getLast(){
	if(etatFantomes[lastMoved] == 1)
	    return lastMoved;
	return -1;
    }

    /**
    *Met une case du labyrinthe à "vide"
    @param : x et y les coordonnées de la case
    */
    public void toEmptyCase(int x, int y){
	if(lab.getCase(x, y).getStatut() != 0)
	    lab.getCase(x, y).setStatut(1);
    }

    /**
    *Fonction qui renvoi true si c'est le juggernaut qui gagne la partie
    */
    public boolean winOfJuggernaut(){
	String [] names=getJoueurs();
	for(int i=0; i<players.size(); i++){
	    if(i!=juggernaut && players.get(names[i])) return false;
	}
	return true;
    }

    /**
    *Revoi true si la partie est finie, false sinon
    */
    public boolean endGame(String nom){
    	if((nbFantomes==0 || players.size()==0) ||
    	   (gameMode==2 && (!live(nom) || winOfJuggernaut())))
    	    return true;
    	return false;
    }

    /**
    *Renvoi l'id du joueur qui a gagné
    */
    public String winner(){
    	if(winOfJuggernaut()) return getJoueurs()[juggernaut];
    	else{
    	    int max=0;
    	    for(int i=0; i<scores.size(); i++){
    		if(scores.get(i)>scores.get(max)){
    		    max=i;
    		}
    	    }
    	    return getJoueurs()[max];
    	}
    }
}
