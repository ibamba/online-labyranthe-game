CC=gcc
CFLAGS= -Wall -pthread -g
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Case.java \
	LectureEcriture.java \
	Labyrinthe.java \
	Game.java \
	PlayerService.java \
	Server.java 

default: classes all

classes: $(CLASSES:.java=.class)

all : joueur.o
	$(CC) $(CFLAGS) joueur.o -o joueur

joueur.o : joueur.h joueur.c
	$(CC) -c $(CFLAGS) joueur.c

clean:
	$(RM) *.class *~
	$(RM) *.o joueur
