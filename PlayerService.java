import java.net.*;
import java.io.*;
import java.util.*;
import java.math.*;

/**
*Ceci est le thread qui va s'occuper de chaque client qui va se connecter
*/

public class PlayerService implements Runnable{

    private Server server;
    private Socket socket;
    private boolean creator;

    public PlayerService(Server serv, Socket s){
	this.server = serv;
	this.socket = s;
	this.creator = false;
    }

    /**
    *On tire un labyrinthe aléatoirement parmi ceux prédéfinis
    */
    public String nomLab(){
	int id = (int)(Math.random() * (4));
	switch(id){
	case 0:
	    return "RAMPAGE.txt";
	case 1:
	    return "PACMAN.txt";
	case 2:
	    return "SMALLBLOCK.txt";
	case 3:
	    return "RUST.txt";
	default:
	    return "PACMAN.txt";
	}
    }

    /**
    *Sert à envoyer un message multi-multidiffusé
    *@param mess : le contenu du message
    *       dso : la socket sur laquelle envoyer les messages 
    *       p : le port d'Envoi
    */
    public void sendMultDiff(String mess, DatagramSocket dso, int p)
	throws Exception{
	byte[]data=mess.getBytes();
	DatagramPacket paquet=new DatagramPacket(data,data.length,server.getGame(p).getInet());
	dso.send(paquet);
    }

    /**
    *Sert à fermer une connexion, donc on ferme un par un tout ce qui compose une connexion
    *@param : p : le port (pour enlever le joueur du serveur)
    *         nom : pour trouver le joueur dont il faut fermer la connexion
    *         pw : le printWriter sur lequel le joueur écrit
    *         br : le bufferedReader sur lequel il lit 
    *         dso : sa socket d'écoute
    */
    public void closeConnexion(int p, String nom, PrintWriter pw, BufferedReader br, DatagramSocket dso)
	throws Exception{
	server.getGame(p).delete(nom);
	server.remPlayer(nom);
	pw.close();
	br.close();
	dso.close();
	socket.close();
    }

    public String toStringTroisO(int i){
	/**
	   Codage des entiers sur 3 octets avec completion avec des 0
	   @arg i : l'entier à coder
	   @return l'entier codé sur 3 octets
	*/
	String res=""+i;
	if(res.length()==1) res = "00"+res;
	else if(res.length()==2) res = "0"+res;
	return res;
    }

    public String toStringQuantreO(int i){
	/**
	   Codage des entiers sur 4 octets avec completion avec des 0
	   @arg i : l'entier à coder
	   @return l'entier codé sur 3 octets
	*/
	String res=""+i;
	if(res.length()==1) res = "000"+res;
	else if(res.length()==2) res = "00"+res;
	else if(res.length()==3) res = "0"+res;
	return res;
    }
    
    /**
    *Cette fonction gère un joueur connecté :
    *     - Les commandes avant de s'enregistrer (NEW, REG, LIST ...)
    *     - Les commandes dans l'acceuil d'avant partie (LIST , ... + JUGGERNAUT,CLASSIC,STEALING pour les modes de jeu si le joueur a créé le jeu)
    *     - Les commandes pendant la partie (UP, DOWN, GLIST, ...)
    *     - Sa déconnexion
    */
    public void run(){
	try{
	    BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    PrintWriter pw=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
	    DatagramSocket dso=new DatagramSocket();
	    String addr=socket.getRemoteSocketAddress().toString().substring(1).split(":")[0];
	    String [] repJoueur;
	    boolean start=false, reg=false, left=false; //jeu commencé, joueur enregistré, est quitté de la partie
	    int votrePartie=0;
	    String nom="";
	    try{
		//Envoi des jeux créés non commencés
		pw.print("GAMES "+server.getNbPartiesNC()+"***");
		pw.flush();
		for(int i=0; i<server.getNbParties(); i++){
		    if(!server.getGame(i).isBegun()){
			pw.print("GAME "+i+" "+server.getGame(i).getJoueurs().length+"***");
			pw.flush();
			Thread.sleep(1);//On laisse le temps au client de recevoir les messages un par un
		    }
		}

		//"Pré-partie" : le joueur peut s'inscrire, voir la taille du labyrinthe,
		//la liste des joueurs de la partie, se désinscrire, changer de mode (extension)
		//lancer la partie
		while(!start){
		    repJoueur=br.readLine().split(" ");

		    if(repJoueur.length>2 && (repJoueur[0].equals("NEW") || repJoueur[0].equals("REG"))){
			nom=repJoueur[1];

			//Creation d'une nouvelle partie
			if(!reg && repJoueur[0].equals("NEW") && repJoueur.length==3){
			    try{
				if(server.newPlayer(nom,
						    Integer.parseInt(repJoueur[2].split("\\*")[0]), addr)){
				    Game g=new Game(server.getAddressMultDif(),server.getPortMultDif(),new Labyrinthe(nomLab()));
				    g.ajouterPlayer(nom);
				    server.addGame(g);
				    votrePartie=server.getNbParties()-1;
				    pw.print("REGOK "+votrePartie+"***");
				    pw.flush();
				    reg=true;
				    creator=true;
				}else{
				    pw.print("REGNO***");
				    pw.flush();
				}
			    }catch(Exception e){
				pw.print("REGNO***");
				pw.flush();
			    }

			    //Enregistrement à une partie existante
			}else if(!reg && repJoueur[0].equals("REG") && repJoueur.length==4){
			    try{
				//On repete les mm operations que precedemment
				//car on doit ajouter le joueur dans la liste
				//du server dans le mm moment qu'on l'ajoute à
				//une partie
				int p=Integer.parseInt(repJoueur[2]);
				votrePartie=Integer.parseInt(repJoueur[3].split("\\*")[0]);
				if(!server.getGame(votrePartie).isBegun() && server.newPlayer(nom, p, addr)){
				    server.getGame(votrePartie).ajouterPlayer(nom);
				    pw.print("REGOK "+votrePartie+"***");
				    pw.flush();
				    reg=true;
				}else{
				    pw.print("REGNO***");
				    pw.flush();
				}
			    }catch(Exception e){
				pw.print("REGNO***");
				pw.flush();
			    }
			}else{
			    pw.print("REGNO***");
			    pw.flush();
			}

		    }else{
			switch(repJoueur[0]){

			    //EXTENSION : Chgmt de mode (mode stealing : les joueurs peuvent se voler les fantômes)
			case "STEALING***":
			    if(reg && creator){
				server.getGame(votrePartie).setMode(1);
				pw.print("STEALING_MODE!***");
				pw.flush();
			    }
			    else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //EXTENSION : Chgmt de mode (mode juggernaut : ce joueur peut manger tous les autres)
			case "JUGGERNAUT***":
			    if(reg && creator){
				server.getGame(votrePartie).setMode(2);
				pw.print("JUGGERNAUT_MODE!***");
				pw.flush();
			    }
			    else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //EXTENSION : Chgmt de mode (classic : le jeu de base)
			    //seul le joueur qui crée la partie peut changer le mode
			case "CLASSIC***":
			    if(reg && creator){
				server.getGame(votrePartie).setMode(0);
				pw.print("CLASSIC_MODE!***");
				pw.flush();
			    }
			    else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //Désinscription du joueur
			case "UNREG***":
			    if(reg){
				if(server.getGame(votrePartie).delete(nom)){
				    server.remPlayer(nom);
				    reg=false;
				    creator=false;
				    pw.print("UNREGOK "+votrePartie+"***");
				    pw.flush();
				}else{
				    pw.print("DUNNO***");
				    pw.flush();
				}
			    }else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //Taille du labyrinthe de la partie
			case "SIZE?":
			    if(repJoueur.length==2){
				try{
				    int i=Integer.parseInt(repJoueur[1].split("\\*")[0]);
				    pw.print("SIZE! "+i+" "+server.getGame(i).getLabTaille()[0]+" "+server.getGame(i).getLabTaille()[1]+"***");
				    pw.flush();
				}catch(Exception e){
				    //si i<0 ou i>nbGame, une exeption sera catchée
				    pw.print("DUNNO***");
				    pw.flush();
				}
			    }else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //Liste des joueurs d'une partie
			case "LIST?":
			    if(repJoueur.length==2){
				try{
				    int i=Integer.parseInt(repJoueur[1].split("\\*")[0]);
				    if(i>=0 && i<=server.getNbParties()){
					String [] j=server.getGame(i).getJoueurs();
					pw.print("LIST! "+i+" "+j.length+"***");
					pw.flush();
					for(String id : j){
					    pw.print("PLAYER "+id+"***");
					    pw.flush();
					    Thread.sleep(1);
					}
				    }else{
					pw.print("DUNNO***");
					pw.flush();
				    }
				}catch(Exception e){
				    pw.print("DUNNO***");
				    pw.flush();
				}
			    }else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //EXTENSION : Mode de la partie
			case "MODE?":
			    if(repJoueur.length==2){
				try{
				    int i=Integer.parseInt(repJoueur[1].split("\\*")[0]);
				    pw.print("MODE "+server.getGame(i).gameModeToString()+"***");
				    pw.flush();
				}catch(Exception e){
				    pw.print("DUNNO***");
				    pw.flush();
				}
			    }else{
				pw.print("DUNNO***");
				pw.flush();
			    }
			    break;

			    //La liste des parties non commencées
			case "GAMES?***":
			    pw.print("GAMES "+server.getNbPartiesNC()+"***");
			    pw.flush();
			    for(int y=0; y<server.getNbParties(); y++){
				if(!server.getGame(y).isBegun()){
				    pw.print("GAME "+y+" "+server.getGame(y).getJoueurs().length+"***");
				    pw.flush();
				    Thread.sleep(1);
				}
			    }
			    break;

			    //Le joueur est prêt pour le debut de la partie
			case "START***" :
			    if(reg){ //Le joueur ne peut envoyer START que s'il est déja enregistré à une partie
				//EXTENSION : Dans le cas du mode juggernaut, la partie necessite au min 2 joueurs
				if(server.getGame(votrePartie).getMode()==2 &&
				   server.getGame(votrePartie).getNbJoueurs()<2){
				    pw.print("2 PLAYERS MIN REQUIRED FOR THIS MODE. WAIT REG***");
				    pw.flush();
				}else{
				    server.getGame(votrePartie).isReady(nom);
				    pw.print("WAITING ALL CONFIRMATIONS***");
				    pw.flush();
				    start=true; //Permet de sortir de la boucle de "pré-partie"
				}
			    }else{
				pw.print("NOT_REG_YET***");
				pw.flush();
			    }
			    break;

			default:
			    pw.print("DUNNO***");
			    pw.flush();
			}
		    }
		}

		//Le joueur attend que tous les autres soient prêts
		synchronized (server){
		    while(!server.getGame(votrePartie).canStart()){
			server.wait();
		    }
		    server.notifyAll();
		}

		//Debut de la partie
		server.getGame(votrePartie).setBegun();
		//Envoie des informations sur la partie
		pw.println("WELCOME "+votrePartie+" "+
			   server.getGame(votrePartie).getLabTaille()[0]+" "+ //hauteur du labyrinthe
			   server.getGame(votrePartie).getLabTaille()[1]+" "+ //largeur du labyrinthe
			   server.getGame(votrePartie).getNbFantomes()+" "+
			   server.getGame(votrePartie).getIp()+" "+
			   server.getGame(votrePartie).getPort()+"***");
		pw.flush();
		Thread.sleep(1);
		//Envoie de la position du joueur
		pw.print("POS "+nom+" "+
			 toStringTroisO(server.getGame(votrePartie).getPos(nom)[1])+" "+
			 toStringTroisO(server.getGame(votrePartie).getPos(nom)[0])+"***");
		pw.flush();

		/**
		   affiche le labyrinthe pour des tests rapide
		   Enlever le commentaire...
		*/
		//server.getGame(votrePartie).affiche();

		//Envoie du mode en multi-diffusion
		if(!server.getGame(votrePartie).getDiff()){
		    server.getGame(votrePartie).setDiff();
		    String mess="##GAME MODE : ";
		    if(server.getGame(votrePartie).getMode()==0){
			mess += "CLASSIC+++";
		    }
		    else if(server.getGame(votrePartie).getMode()==1){
			mess += "STEALING\n"+
			    "##DESC :\n"+
			    " #THE LAST PLAYER WHO MOVED ON THE CASE STEALS THE POINTS OF THE OTHER ALSO ON THE CASE\n"+
			    " #THE TOTAL SCORE OF PLAYER WHO WAS THERE BEFORE+++";
		    }
		    else if(server.getGame(votrePartie).getMode()==2){
			mess += "JUGGERNAUT\n##PLAYER_JUGGER : "+server.getGame(votrePartie).getNomJuggernaut()+
			    "\n##DESC :\n"+
			    " #JUGGER CATCH YOU : YOU LOSE\n"+
			    " #ALL PLAYERS CAUGHT : WIN OF JUGGERNAUT\n"+
			    " #NO MORE GHOSTS IN THE MAZE : WIN OF PLAYER WITH MAX SCORE+++";
		    }
		    sendMultDiff(mess, dso, votrePartie);
		}

		repJoueur = br.readLine().split(" ", 3);
		//Boucle principale : déroulement du jeu
		while(!left && !server.getGame(votrePartie).endGame(nom)){

		    //Déplacement du joueur
		    if(repJoueur[0].equals("LEFT") || repJoueur[0].equals("UP")
		       || repJoueur[0].equals("RIGHT") || repJoueur[0].equals("DOWN")){
			int dir=0;

			//Enregistrement de la direction du déplacement
			switch(repJoueur[0]){
			case "LEFT":
			    dir=0;
			    break;
			case "UP":
			    dir=1;
			    break;
			case "RIGHT":
			    dir=2;
			    break;
			case "DOWN":
			    dir=3;
			    break;
			default:
			    pw.print("NOT MOVED***");
			    pw.flush();
			    break;
			}
			//Exécution du déplacement
			if(repJoueur.length==2){
			    try{
				int [] coord=server.getGame(votrePartie).movePlayer(nom,Integer.parseInt(repJoueur[1].split("\\*")[0]), dir);

				//Le joueur a capturé des points dans le mode normal ou le mode stealing
				if(coord[2] == 0 || coord[2] == 1){
				    String mess;
				    if(coord[2] == 0){//Le joueur a capturé un fantome
					pw.print("MOF "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+" "+
						 toStringQuantreO(coord[3])+"***");
					pw.flush();
					mess = "SCOR "+nom+" "+toStringQuantreO(coord[3])+" "+toStringTroisO(coord[1])+" "+
					    toStringTroisO(coord[0])+"+++";
				    }else{//Le joueur a capturé les points d'un autre
					pw.print("MOJ "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+" "+
						 toStringQuantreO(coord[3])+"***");
					pw.flush();
					mess = "PLAYER "+nom+" ATE PLAYER AT POS "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+
					    "\n"+nom+" SCOR "+toStringQuantreO(coord[3])+"+++";
				    }
				    Thread.sleep(1);
				    sendMultDiff(mess, dso, votrePartie);
				}

				//Mode juggernaut
				else if(coord[2] == 2){
				    if(coord[3] != -1){
					String name = server.getGame(votrePartie).getJoueurs()[coord[3]];
					if(nom.equals(server.getGame(votrePartie).getNomJuggernaut())){
					    pw.print("PLAYER ANNIHILATION***");
					    pw.flush();
					}else{
					    pw.print("RIP***");
					    pw.flush();
					}
					Thread.sleep(1);
					String mess = name+" AS FALLEN+++";
					sendMultDiff(mess, dso, votrePartie);
				    }
				    else{
					pw.print("MOV "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+"***");
					pw.flush();
				    }
				    String mess = "JUGGERNAUT "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+"+++";
				    byte[]data=mess.getBytes();
				    DatagramPacket paquet=new DatagramPacket(data,data.length);
				    paquet=new DatagramPacket(data,data.length,server.getGame(votrePartie).getInet());
				    dso.send(paquet);
				}

				//Le joueur n'a capturé aucun point
				else{
				    pw.print("MOV "+toStringTroisO(coord[1])+" "+toStringTroisO(coord[0])+"***");
				    pw.flush();
				}

				//Multidiffusion du déplacement d'un fantôme
				int fantom;
				if((fantom=server.getGame(votrePartie).getLast()) != -1){
				    int coord2 [] = server.getGame(votrePartie).getPos(fantom);
				    String messMD = "FANT "+toStringTroisO(coord2[1])+" "+toStringTroisO(coord2[0])+"+++";
				    sendMultDiff(messMD, dso, votrePartie);
				}

			    }catch(NumberFormatException e){
				pw.print("NOT MOVED***");
				pw.flush();
			    }

			}else{
			    pw.print("NOT MOVED***");
			    pw.flush();
			}

			/**
			   affiche le labyrinthe pour des tests rapide
			   Enlever le commentaire...
			*/
			//server.getGame(votrePartie).affiche();
		    }

		    else{
			switch(repJoueur[0]){

			    //la liste des joueurs de la partie en cours
			case "GLIST?***":
			    String [] j=server.getGame(votrePartie).getJoueurs();
			    pw.print("GLIST! "+j.length+"***");
			    pw.flush();
			    for(String id : j){
				int [] posJ = server.getGame(votrePartie).getPos(id);
				pw.print("GPLAYER "+id+" "+toStringTroisO(posJ[0])+" "+toStringTroisO(posJ[1])+" "+server.getGame(votrePartie).getPoints(id)+"***");
				pw.flush();
				Thread.sleep(1);
			    }
			    break;

			    //EXTENSION : le mode de la partie en cours
			case "GMODE?***":
			    pw.print("GMODE! "+server.getGame(votrePartie).gameModeToString()+"***");
			    pw.flush();
			    break;

			    //EXTENSION : le score du joueur dans la partie
			case "SCOR?***":
			    pw.print("SCOR! "+nom+" "+toStringQuantreO(server.getGame(votrePartie).getPoints(nom))+"***");
			    pw.flush();
			    break;

			    //Message multidiffusé dans la partie
			case "ALL?":
			    String mess="";
			    if(repJoueur.length>=2){
				if(repJoueur.length==2){
				    mess="MESA "+nom+" "+repJoueur[1].split("\\*")[0]+"+++";
				}else if(repJoueur.length==3){
				    mess="MESA "+nom+" "+repJoueur[1]+" "+repJoueur[2].split("\\*")[0]+"+++";
				}
				sendMultDiff(mess, dso, votrePartie);
				pw.print("ALL!***");
				pw.flush();
			    }else{
				pw.print("NOSEND***");
				pw.flush();
			    }
			    break;

			    //envoi d'un message personnel :
			    //Un joueur peut envoyer un message personnel à tout joueur s'étant enregistré sur le serveur,
			    //qu'il fasse partie de la même partie que lui ou non, une fois sa partie lancée
			case "SEND?":
			    if(repJoueur.length==3){
				try{
				    mess="MESP "+nom+" "+repJoueur[2].split("\\*")[0]+"+++";
				    byte[]data=mess.getBytes();
				    Integer p;
				    if((p=server.getPortPlayer(repJoueur[1]))!=null){
					DatagramPacket paquet=new DatagramPacket(data,data.length,
										 InetAddress.getByName(server.getAddrPlayer(repJoueur[1])),p);
					dso.send(paquet);
					pw.print("SEND!***");
					pw.flush();
				    }else{
					pw.print("NOSEND!***");
					pw.flush();
				    }
				} catch(Exception e){
				    pw.print("NOSEND!***");
				    pw.flush();
				}
			    }else{
				pw.print("NOSEND!***");
				pw.flush();
			    }
			    break;

			    //Sortie d'une partie en cours...
			case "QUIT***":
			    pw.print("BYE***");
			    pw.flush();
			    left=true;
			    //suppression de l'image du joueur dans le labyrinthe
			    server.getGame(votrePartie).toEmptyCase(server.getGame(votrePartie).getPos(nom)[0],
								    server.getGame(votrePartie).getPos(nom)[1]);
			    System.out.println("PLAYER "+nom+" HAS LEFT");
			    closeConnexion(votrePartie, nom, pw, br, dso);
			    break;
			default:
			    pw.print("DUNNO***");
			    pw.flush();
			    break;
			}
		    }
		    if(!left) repJoueur = br.readLine().split(" ", 3);
		}

		//Fin de la partie

		//le joueur n'a pas quitté la partie
		if(!left){
		    if(server.getGame(votrePartie).live(nom)){
			//Envoie du nom du vainquer en multidiffusion
			if(server.getGame(votrePartie).getNbJoueurs()>0 && !server.getGame(votrePartie).getEnd()){
			    server.getGame(votrePartie).setEnd();
			    String vainqueur = server.getGame(votrePartie).winner();
			    String mess = "END "+vainqueur+" "+toStringQuantreO(server.getGame(votrePartie).getPoints(vainqueur))+"+++";
			    sendMultDiff(mess, dso, votrePartie);
			}
			pw.print("BYE***");
			pw.flush();
			closeConnexion(votrePartie, nom, pw, br, dso);
		    }else{
			pw.print("BYE***");
			pw.flush();
			closeConnexion(votrePartie, nom, pw, br, dso);
		    }
		}
	    } catch(Exception e){
		//traitement d'une coupure de connexion inatendue
		System.out.println("PLAYER "+nom+" HAS LEFT");
		if(server.getNbParties()>0){
		    server.getGame(votrePartie).delete(nom);
		    server.getPlayers().remove(nom);
		}
		pw.close();
		br.close();
		dso.close();
		socket.close();
	    }
	} catch(Exception e){
	    System.out.println(e);
	    e.printStackTrace();
	}
    }
}
