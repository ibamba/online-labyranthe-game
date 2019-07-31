#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <pthread.h>
#include <netinet/ip.h>
#include <ctype.h>

#include "joueur.h"

int port=-1;
char ip [15]="";
char ip_serv [15];

pthread_cond_t condition = PTHREAD_COND_INITIALIZER;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

/**
*Le thread qui va gérer l'enregistrement d'un joueur
*@param : id l'id du joueur lors du lancement du programme, p son port
*/
joueur* creer_joueur(char i[9], int p){
  joueur* res= (joueur*)malloc(sizeof(joueur));
  strcpy(res->id,i);
  res->port=p;
  return res;
}

/**
*Vérifie que le pseudo utilisé lors de son enregistrement est le même utilisé lors du lancement du programme
*@param : id l'id que le joueur a entré
*/
int verify_id(char id[9]){
  int i;
  if(strlen(id)>8) return 0;
  for(i=0; i<strlen(id); i++){
    if(!isalnum(id[i])) return 0;
  }
  return 1;
}

/**
*Vérifie que le joueur a bien donné les bonnes informations
*@param : j le joueur, n le pseudo qu'il a entré pour s'enregistrer, p le port qu'il a entré pour s'enregistrer
*/
int verify_infos_player(joueur *j, char* n, int p){
  if(n==NULL || p==0 || p!=j->port || strcmp(n, j->id))
    return 0;
  return 1;
}

/**
*Réception des messages privés
*@param : jo le thread de réception
*/
void* receive_private(void* jo){
  joueur* j=(joueur*)jo;
  int sock=socket(PF_INET,SOCK_DGRAM,0);
  sock=socket(PF_INET,SOCK_DGRAM,0);
  struct sockaddr_in address_sock;
  address_sock.sin_family=AF_INET;
  address_sock.sin_port=htons(j->port);
  address_sock.sin_addr.s_addr=htonl(INADDR_ANY);
  int r=bind(sock,(struct sockaddr*)&address_sock,sizeof(struct sockaddr_in));
  if(r==0){
    char tampon[250];
    while(1){
      int rec=recv(sock,tampon,250,0);
      tampon[rec]='\0';
      printf("NOUVEAU MESSAGE PERSONNEL RECU : %s\n",tampon);
    }
  }
  return NULL;
}

/**
*Réception des messages multidiffusés
*@param : arg le thread de réception
*/
void* read_multidif(void* arg){
  pthread_mutex_lock(&mutex);
  pthread_cond_wait(&condition,&mutex);
  pthread_mutex_unlock(&mutex);
  int sock=socket(PF_INET,SOCK_DGRAM,0);
  int ok=1;
  setsockopt(sock,SOL_SOCKET,SO_REUSEPORT,&ok,sizeof(ok));
  struct sockaddr_in address_sock;
  address_sock.sin_family=AF_INET;
  address_sock.sin_port=htons(port);
  address_sock.sin_addr.s_addr=htonl(INADDR_ANY);
  bind(sock,(struct sockaddr *)&address_sock,sizeof(struct sockaddr_in));
  struct ip_mreq mreq;
  mreq.imr_multiaddr.s_addr=inet_addr(ip);
  mreq.imr_interface.s_addr=htonl(INADDR_ANY);
  setsockopt(sock,IPPROTO_IP, IP_ADD_MEMBERSHIP,&mreq,sizeof(mreq));
  char tampon[200];
  while(1){
    int rec=recv(sock,tampon,200,0);
    tampon[rec]='\0';
    printf("\n%s\n",tampon);
  }
  return NULL;
}

/**
*Le thread qui s'occupe d'envoyer toutes les commandes du joueur au serveur tant qu'il est connecté,
*et il reçoit les réponses du serveur suites aux messages que le joueur a pu envoyer
*/
void* jouer(void* jo){
  joueur* j=(joueur*)jo;
  struct sockaddr_in adress_sock;
  adress_sock.sin_family = AF_INET;
  adress_sock.sin_port = htons(3773);
  inet_aton(ip_serv,&adress_sock.sin_addr);
  int descr=socket(PF_INET,SOCK_STREAM,0);
  int r=connect(descr,(struct sockaddr*)&adress_sock,sizeof(struct sockaddr_in));
  if(r!=-1){
    char buff[200];
    char mess[200];
    int size_rec;
    int i, flag=1, already_verified=0;
    size_rec=read(descr,buff,200*sizeof(char));
    buff[size_rec]='\0';
    printf("%s\n",buff);
    char *s=strtok(buff, " ");
    while(strcmp(s, "WAITING")){
      int nbmess=0;
      if(s!=NULL && !strcmp(s, "REGNO***\n")) already_verified=0;
      else if(s!=NULL && !strcmp(s, "LIST!")){
	s=strtok(NULL, " ");
	if(s!=NULL) nbmess=atoi(strtok(NULL, "*"));
      }
      else if(s!=NULL && !strcmp(s, "GAMES")){
	s=strtok(NULL, "*");
	if(s!=NULL) nbmess=atoi(s);
      }
      if(nbmess!=0){
	for(i=0; i<nbmess; i++){
	  size_rec=read(descr,buff,200*sizeof(char));
	  buff[size_rec]='\0';
	  printf("%s\n",buff);
	}
      }
      fgets(mess, sizeof(mess), stdin);
      char*msg_temp=(char*)malloc(sizeof(char)*strlen(mess));
      strcpy(msg_temp, mess);
      char* m=strtok(msg_temp, " ");
      if(!already_verified && (!strcmp(m, "NEW") || !strcmp(m, "REG"))){
	char* n;
	int p;
	flag=0;
	if(m!=NULL) n=strtok(NULL, " ");
	else n=NULL;
	if(n!=NULL){
	  m=strtok(NULL, " ");
	  if(m!=NULL) p=atoi(m);
	  else p=0;
	}
	else p=0;
	if(!verify_infos_player(j, n, p)){
	  printf("This id and port are not the same than yours\n\n");
	}else{
	  flag=1;
	  already_verified=1;
	}
      }
      if(flag){
	send(descr,mess,strlen(mess),0);
	size_rec=read(descr,buff,200*sizeof(char));
	buff[size_rec]='\0';
	printf("%s\n",buff);
	s=strtok(buff, " ");
      }
    }
    size_rec=read(descr,buff,200*sizeof(char));
    buff[size_rec]='\0';
    printf("%s\n",buff);
    char * ip2=strtok(buff, " ");
    for(i=0; i<5; i++){
      if(ip2!=NULL){
	ip2=strtok(NULL, " ");
      }
    }

    strcpy(ip,ip2);
    char*tmp=strtok(NULL,"*");
    port = atoi(tmp);
    s=strtok(buff, " ");
    pthread_mutex_lock(&mutex);
    pthread_cond_signal(&condition);
    pthread_mutex_unlock(&mutex);
    while(1){
      if(s!=NULL && !strcmp(s, "WELCOME")){
	size_rec=read(descr,buff,200*sizeof(char));
	buff[size_rec]='\0';
	printf("%s\n",buff);
      }
      fgets(mess, sizeof(mess), stdin);
      send(descr,mess,strlen(mess),0);
      size_rec=read(descr,buff,200*sizeof(char));
      buff[size_rec]='\0';
      printf("%s\n",buff);
      if(!strcmp(mess, "GLIST?***\n")){
	s=strtok(buff, " ");
	int tmp=atoi(strtok(NULL, "*"));
	for(i=0; i<tmp; i++){
	  size_rec=read(descr,buff,200*sizeof(char));
	  buff[size_rec]='\0';
	  printf("%s\n",buff);
	}
      }
    }
  }
  return NULL;
}

int main(int argc, char** argv){
  if(argc!=4)
    printf("Too few argument.\n"
	   "Using : ./joueur <Id:char* (max 8 alpha num characters)> <port:int> <ip server:char*>\n");
  else if(!verify_id(argv[1]) || atoi(argv[2])<1024 || atoi(argv[2])>49151)
    printf("identifiant must be less than 9 alpha num characters and port between 1024 and 49151\n");
  else{
    joueur* j=creer_joueur(argv[1], atoi(argv[2]));
    strcpy(ip_serv, argv[3]);
    pthread_t jeu, recpt, multicast;
    pthread_create(&jeu,NULL,jouer,j);
    pthread_create(&recpt,NULL,receive_private,j);
    pthread_create(&multicast,NULL,read_multidif,NULL);
    pthread_join(jeu,NULL);
    pthread_join(recpt,NULL);
    pthread_join(multicast,NULL);
  }
}
