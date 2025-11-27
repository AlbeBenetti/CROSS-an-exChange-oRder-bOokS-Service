**Struttura progetto**

I files .java necessari per l’esecuzione del progetto sono divisi in 4 packages:
  crossClient ⇒ contiene i file sorgenti per l’esecuzione del client, in paticolare ha ClientMain.java che rappresenta il main del client e ClientCROSS.java che formula le richieste da inviare poi al server
  crossServer ⇒ contiene i sorgenti per eseguire il lato server dell’applicazione, in particolare ServerMain.java, il quale rimane in attesa di client che cercano di connettersi al servizio e, una volta ricevuti genera un'istanza di CrossExecutor per comunicare con il thread in un altro thread
  orderTypes ⇒ contiene le classi necessarie per definire gli ordini e l’OrderBook, necessario per memorizzare gli ordini non ancora evasi. Questo package viene utilizzato dal server per la gestione degli ordini
  requestMessages ⇒ contiene le classi necessarie per inviare e ricevere oggetti Json che rappresentano richieste (dal client per il server) e risposte (dal server verso il client)

Le due classi principali del progetto sono ClientMain.java e ServerMain.java
In più nel progetto è presente la cartella configs, la quale contiene i file di configurazione del server e del client e la cartella libs contenente la libreria GSON

**Istruzioni per la compilazione**

Grazie al Makefile che è stato generato è possibile compilare tutto grazie al comando make all.
Se invece vogliamo soltanto ripulire i file e le directory date dalla compilazione è presente il comando make clean
Infine per eseguire il server è necessario il comando make run-server, mentre per eseguire il client è necessario il comando make run-client
