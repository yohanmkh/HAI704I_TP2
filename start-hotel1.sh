#!/bin/bash
# Démarrage de l'Hôtel 1 sur le port 8080
cd "$(dirname "$0")/hotel" || exit 1
mvn exec:exec -Dport=8080 -Dprofile=hotel1
