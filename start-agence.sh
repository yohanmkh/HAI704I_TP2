#!/bin/bash
# Démarrage du client Agence CLI
cd "$(dirname "$0")/agence" || exit 1
mvn exec:exec
