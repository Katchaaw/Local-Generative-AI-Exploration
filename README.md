# Modèle génératif local pour l’exploration et la chasse collaborative

Ce projet explore l'intégration de modèles de langage légers exécutés localement (*Small Language Models* - SLM) comme moteurs de décision au sein d'un Système Multi-Agents (SMA). Développé sur la plateforme de simulation géométrique **Dédale** (basée sur le framework **JADE**), l'objectif est de coordonner une escouade d'agents autonomes pour cartographier un environnement inconnu et encercler de manière collaborative un adversaire mobile (le Golem / Wumpus).

## 🛠️ Prérequis

Le projet impose une **exécution 100% locale**, garantissant l'indépendance vis-à-vis des API cloud et la confidentialité des données.

* **Langage :** Java (OpenJDK 21 ou supérieur)
* **Framework SMA :** JADE (Java Agent Development Framework)
* **Environnement :** Plateforme Dédale
* **Orchestration LLM :** LangChain4j (v0.36.0)
* **Serveur d'inférence :** Ollama
* **Modèle cible :** `llama3.2:3b` (3 milliards de paramètres)

### Configuration d'Ollama
Avant de lancer la simulation JADE, assurez-vous d'avoir installé Ollama et téléchargé le modèle requis :
```bash
ollama pull llama3.2:3b
```
Enfin, il suffit de le lancer dans votre terminal avant d'exécuter le fichier `Principal.java` :
```bash
ollama run llama3.2:3b
```