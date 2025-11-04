JAVAC = javac
JAVA = java

# Directory
SRC_DIR = src
BIN_DIR = bin
LIB_DIR = lib
RES_DIR = resources

# Librerie esterne
GSON_JAR = $(LIB_DIR)/gson-2.13.1.jar

# Classpath
CLASSPATH = $(BIN_DIR):$(GSON_JAR)

# Entry points
CLIENT_MAIN = crossClient.ClientMain
SERVER_MAIN = crossServer.ServerMain

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

all: clean compile

compile:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -cp $(GSON_JAR) -d $(BIN_DIR) $(SOURCES)
	
clean:
	@test -n $(BIN_DIR) || (echo "ERRORE: BIN_DIR non definita!" && exit 1)
	rm -rf $(BIN_DIR)/*
	
run-client:
	$(JAVA) -cp $(CLASSPATH) $(CLIENT_MAIN)
	
run-server:
	$(JAVA) -cp $(CLASSPATH) $(SERVER_MAIN)
