#!/bin/bash
# Démarrage de l'Hôtel 2 sur le port 8081
cd "$(dirname "$0")/hotel" || exit 1
mvn exec:exec -Dport=8081 -Dprofile=hotel2
