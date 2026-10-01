# Local Generative Model for Collaborative Exploration and Hunting

This project explores the integration of locally deployed lightweight language models (Small Language Models — SLMs) as decision-making engines within a Multi-Agent System (MAS). Developed on the Dédale geometric simulation platform (based on the JADE framework), the objective is to coordinate a squad of autonomous agents to map an unknown environment and collaboratively surround a mobile opponent (the Golem / Wumpus).

## 🛠️ Requirements

The project requires **100% local execution**, ensuring independence from cloud APIs and preserving data privacy.

* **Language:** Java (OpenJDK 21 or later)
* **MAS Framework:** JADE (Java Agent Development Framework)
* **Environment:** Dédale plateform
* **LLM Orchestration:** LangChain4j (v0.36.0)
* **Inference Server:** Ollama
* **Target Model: ** `llama3.2:3b` (3 billion parameters)

### Ollama Configuration
Before launching the JADE simulation, make sure Ollama is installed and the required model has been downloaded:
```bash
ollama pull llama3.2:3b
```
Finally, simply run it in your terminal before executing the Principal.java file:
```bash
ollama run llama3.2:3b
```
